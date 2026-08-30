package com.example.ultimatetracker.data.repository

import android.content.SharedPreferences
import androidx.room.withTransaction
import com.example.ultimatetracker.data.local.AppDatabase
import com.example.ultimatetracker.data.local.account.AccountConstants
import com.example.ultimatetracker.data.local.account.AccountDao
import com.example.ultimatetracker.data.local.account.AccountEntity
import com.example.ultimatetracker.data.local.account.AuditEventEntity
import com.example.ultimatetracker.data.local.account.SessionEntity
import com.example.ultimatetracker.data.local.account.UserEntity
import com.example.ultimatetracker.data.local.account.UserListEntity
import com.example.ultimatetracker.data.local.account.UserProfileEntity
import com.example.ultimatetracker.security.PasswordHasher
import com.example.ultimatetracker.security.SessionTokenService
import com.example.ultimatetracker.security.isValidEmail
import com.example.ultimatetracker.security.isValidPassword
import com.example.ultimatetracker.security.normalizeEmail
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.ExperimentalCoroutinesApi

data class SignedInUser(
    val userId: Long,
    val displayName: String,
    val email: String?,
    val isGuest: Boolean,
    val sessionId: Long,
    val activeListId: Long,
    val avatarUri: String? = null,
)

sealed interface AccountResult {
    data object Success : AccountResult
    data object InvalidEmail : AccountResult
    data object WeakPassword : AccountResult
    data object EmailAlreadyUsed : AccountResult
    data object InvalidCredentials : AccountResult
    data class Locked(val until: Long) : AccountResult
    data object Conflict : AccountResult
    data object NotAllowed : AccountResult
    data object NotFound : AccountResult
}

@OptIn(ExperimentalCoroutinesApi::class)
class AccountRepository(
    private val database: AppDatabase,
    private val preferences: SharedPreferences,
    private val identityStore: ActiveIdentityStore,
    private val deviceName: String,
    private val passwordHasher: PasswordHasher = PasswordHasher(),
    private val tokenService: SessionTokenService = SessionTokenService(),
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val dao: AccountDao = database.accountDao()
    private val _currentUser = MutableStateFlow<SignedInUser?>(null)
    val currentUser: StateFlow<SignedInUser?> = _currentUser.asStateFlow()

    val lists: Flow<List<UserListEntity>> = currentUser.flatMapLatest { user ->
        if (user == null) flowOf(emptyList()) else dao.observeLists(user.userId)
    }

    val profile: Flow<UserProfileEntity?> = currentUser.flatMapLatest { user ->
        if (user == null) flowOf(null) else dao.observeProfile(user.userId)
    }

    val accounts: Flow<List<AccountEntity>> = currentUser.flatMapLatest { user ->
        if (user == null) flowOf(emptyList()) else dao.observeAccounts(user.userId)
    }

    fun sessions(): Flow<List<SessionEntity>> {
        val user = currentUser.value ?: return flowOf(emptyList())
        return dao.observeSessions(user.userId, now())
    }

    suspend fun initialize() {
        ensureGuest()
        val rawToken = preferences.getString(ACTIVE_TOKEN, null) ?: return
        val time = now()
        val session = dao.validSession(tokenService.hash(rawToken), time) ?: run {
            clearLocalSession()
            return
        }
        val user = dao.activeUser(session.userId) ?: run {
            clearLocalSession()
            return
        }
        val listId = preferences.getLong(ACTIVE_LIST, 0).takeIf { it > 0 }
            ?.let { dao.ownedList(user.id, it) }?.id
            ?: ensureDefaultList(user.id)
        dao.touchSession(session.id, time)
        activate(user, session.id, listId, rawToken)
    }

    suspend fun continueAsGuest(): AccountResult {
        ensureGuest()
        val user = dao.activeUser(AccountConstants.GUEST_USER_ID) ?: return AccountResult.NotFound
        val listId = ensureDefaultList(user.id)
        issueAndActivate(user, listId, "guest.continue")
        return AccountResult.Success
    }

    suspend fun register(email: String, password: String, displayName: String): AccountResult {
        if (!isValidEmail(email)) return AccountResult.InvalidEmail
        if (!isValidPassword(password)) return AccountResult.WeakPassword
        val normalized = normalizeEmail(email)
        if (dao.accountByEmail(normalized) != null) return AccountResult.EmailAlreadyUsed
        val time = now()
        val digest = passwordHasher.hash(password.toCharArray())
        lateinit var createdUser: UserEntity
        var listId = 0L
        try {
            database.withTransaction {
                val userId = dao.insertUser(UserEntity(status = AccountConstants.ACTIVE, isGuest = false, createdAt = time, updatedAt = time))
                createdUser = UserEntity(userId, AccountConstants.ACTIVE, false, time, time)
                dao.insertProfile(UserProfileEntity(userId, displayName.trim().ifBlank { normalized.substringBefore('@') }, "", time, time))
                dao.insertAccount(AccountEntity(
                    userId = userId,
                    provider = AccountConstants.LOCAL_PROVIDER,
                    providerAccountId = normalized,
                    emailNormalized = normalized,
                    passwordHash = digest.hash,
                    passwordSalt = digest.salt,
                    passwordIterations = digest.iterations,
                    createdAt = time,
                    updatedAt = time,
                ))
                val guestHasActiveData = _currentUser.value?.isGuest == true
                if (guestHasActiveData) {
                    dao.transferGuestLists(AccountConstants.GUEST_USER_ID, userId, time)
                    listId = dao.firstActiveList(userId)?.id ?: createDefaultList(userId, time)
                    createDefaultList(AccountConstants.GUEST_USER_ID, time)
                } else {
                    listId = createDefaultList(userId, time)
                }
                audit(userId, "account.register", "user", userId, time)
            }
        } catch (_: android.database.sqlite.SQLiteConstraintException) {
            return AccountResult.EmailAlreadyUsed
        }
        issueAndActivate(createdUser, listId, "session.created")
        return AccountResult.Success
    }

    suspend fun login(email: String, password: String): AccountResult {
        val normalized = normalizeEmail(email)
        val account = dao.accountByEmail(normalized) ?: return AccountResult.InvalidCredentials
        val time = now()
        account.lockedUntil?.takeIf { it > time }?.let { return AccountResult.Locked(it) }
        val valid = account.passwordHash != null && account.passwordSalt != null && account.passwordIterations != null &&
            passwordHasher.verify(password.toCharArray(), account.passwordHash, account.passwordSalt, account.passwordIterations)
        if (!valid) {
            val attempts = account.failedLoginAttempts + 1
            val lockedUntil = if (attempts >= MAX_FAILED_LOGINS) time + LOCK_DURATION_MS else null
            dao.updateAccount(account.copy(
                failedLoginAttempts = if (lockedUntil == null) attempts else 0,
                lockedUntil = lockedUntil,
                updatedAt = time,
            ))
            return lockedUntil?.let(AccountResult::Locked) ?: AccountResult.InvalidCredentials
        }
        val user = dao.activeUser(account.userId) ?: return AccountResult.InvalidCredentials
        dao.updateAccount(account.copy(failedLoginAttempts = 0, lockedUntil = null, updatedAt = time))
        val listId = ensureDefaultList(user.id)
        issueAndActivate(user, listId, "session.created")
        return AccountResult.Success
    }

    suspend fun logout(): AccountResult {
        val user = currentUser.value ?: return AccountResult.NotFound
        dao.revokeSession(user.userId, user.sessionId, now())
        audit(user.userId, "session.revoked", "session", user.sessionId)
        clearLocalSession()
        return AccountResult.Success
    }

    suspend fun logoutAll(): AccountResult {
        val user = currentUser.value ?: return AccountResult.NotFound
        dao.revokeAllSessions(user.userId, now())
        audit(user.userId, "session.revoked_all", "user", user.userId)
        clearLocalSession()
        return AccountResult.Success
    }

    suspend fun revokeSession(sessionId: Long): AccountResult {
        val user = currentUser.value ?: return AccountResult.NotFound
        if (dao.revokeSession(user.userId, sessionId, now()) != 1) return AccountResult.NotFound
        if (sessionId == user.sessionId) clearLocalSession()
        return AccountResult.Success
    }

    suspend fun updateProfile(displayName: String, locale: String, avatarUri: String? = null): AccountResult {
        val user = currentUser.value ?: return AccountResult.NotFound
        val profile = dao.profile(user.userId) ?: return AccountResult.NotFound
        val name = displayName.trim()
        if (name.isEmpty() || name.length > 80) return AccountResult.NotAllowed
        val savedAvatar = avatarUri ?: profile.avatarUri
        dao.updateProfile(profile.copy(displayName = name, locale = locale.take(16), avatarUri = savedAvatar, updatedAt = now()))
        _currentUser.value = user.copy(displayName = name, avatarUri = savedAvatar)
        return AccountResult.Success
    }

    suspend fun createList(title: String): AccountResult {
        val user = currentUser.value ?: return AccountResult.NotFound
        val normalized = title.trim()
        if (normalized.isEmpty() || normalized.length > 80) return AccountResult.NotAllowed
        val time = now()
        val id = dao.insertList(UserListEntity(ownerUserId = user.userId, title = normalized, position = dao.nextListPosition(user.userId), createdAt = time, updatedAt = time))
        audit(user.userId, "list.created", "list", id, time)
        if (dao.ownedList(user.userId, user.activeListId) == null) selectList(id)
        return AccountResult.Success
    }

    suspend fun selectList(listId: Long): AccountResult {
        val user = currentUser.value ?: return AccountResult.NotFound
        val list = dao.ownedList(user.userId, listId) ?: return AccountResult.NotFound
        if (list.archivedAt != null) return AccountResult.NotAllowed
        setActiveList(user, list.id)
        return AccountResult.Success
    }

    suspend fun renameList(listId: Long, title: String, expectedVersion: Long): AccountResult {
        val user = currentUser.value ?: return AccountResult.NotFound
        val normalized = title.trim()
        if (normalized.isEmpty() || normalized.length > 80) return AccountResult.NotAllowed
        return if (dao.renameList(user.userId, listId, normalized, expectedVersion, now()) == 1) AccountResult.Success else AccountResult.Conflict
    }

    suspend fun moveList(listId: Long, direction: Int): AccountResult {
        val user = currentUser.value ?: return AccountResult.NotFound
        val list = dao.ownedList(user.userId, listId) ?: return AccountResult.NotFound
        val targetPosition = list.position + direction.coerceIn(-1, 1)
        if (targetPosition < 0) return AccountResult.NotAllowed
        val other = dao.listAtPosition(user.userId, targetPosition) ?: return AccountResult.NotAllowed
        val time = now()
        database.withTransaction {
            dao.setListPosition(user.userId, other.id, list.position, time)
            dao.setListPosition(user.userId, list.id, targetPosition, time)
        }
        return AccountResult.Success
    }

    suspend fun archiveList(listId: Long, archived: Boolean): AccountResult {
        val user = currentUser.value ?: return AccountResult.NotFound
        if (dao.setListArchived(user.userId, listId, if (archived) now() else null, now()) != 1) return AccountResult.NotFound
        if (archived && user.activeListId == listId) selectFallbackList(user)
        return AccountResult.Success
    }

    suspend fun deleteList(listId: Long): AccountResult {
        val user = currentUser.value ?: return AccountResult.NotFound
        if (dao.softDeleteList(user.userId, listId, now()) != 1) return AccountResult.NotFound
        if (user.activeListId == listId) selectFallbackList(user)
        audit(user.userId, "list.deleted", "list", listId)
        return AccountResult.Success
    }

    suspend fun deleteAccount(): AccountResult {
        val user = currentUser.value ?: return AccountResult.NotFound
        if (user.isGuest) return AccountResult.NotAllowed
        val time = now()
        database.withTransaction {
            dao.disableAccounts(user.userId, time)
            dao.revokeAllSessions(user.userId, time)
            dao.softDeleteUser(user.userId, time)
            audit(user.userId, "account.deleted", "user", user.userId, time)
        }
        clearLocalSession()
        return AccountResult.Success
    }

    private suspend fun ensureGuest() {
        if (dao.activeUser(AccountConstants.GUEST_USER_ID) != null) return
        val time = now()
        database.withTransaction {
            dao.insertUserIfMissing(UserEntity(AccountConstants.GUEST_USER_ID, AccountConstants.ACTIVE, true, time, time))
            dao.insertProfileIfMissing(UserProfileEntity(AccountConstants.GUEST_USER_ID, "Guest", "", time, time))
            if (dao.firstActiveList(AccountConstants.GUEST_USER_ID) == null) createDefaultList(AccountConstants.GUEST_USER_ID, time)
        }
    }

    private suspend fun ensureDefaultList(userId: Long): Long =
        dao.firstActiveList(userId)?.id ?: createDefaultList(userId, now())

    private suspend fun createDefaultList(userId: Long, time: Long): Long =
        dao.insertList(UserListEntity(ownerUserId = userId, title = "My list", position = dao.nextListPosition(userId), createdAt = time, updatedAt = time))

    private suspend fun issueAndActivate(user: UserEntity, listId: Long, action: String) {
        val rawToken = tokenService.create()
        val time = now()
        val sessionId = dao.insertSession(SessionEntity(
            userId = user.id,
            tokenHash = tokenService.hash(rawToken),
            deviceName = deviceName.take(120),
            createdAt = time,
            lastUsedAt = time,
            expiresAt = time + SESSION_LIFETIME_MS,
        ))
        audit(user.id, action, "session", sessionId, time)
        activate(user, sessionId, listId, rawToken)
    }

    private suspend fun activate(user: UserEntity, sessionId: Long, listId: Long, rawToken: String) {
        val profile = dao.profile(user.id)
        preferences.edit().putString(ACTIVE_TOKEN, rawToken).putLong(ACTIVE_LIST, listId).apply()
        val signedIn = SignedInUser(user.id, profile?.displayName.orEmpty(), dao.primaryAccount(user.id)?.emailNormalized, user.isGuest, sessionId, listId, profile?.avatarUri)
        _currentUser.value = signedIn
        identityStore.set(ActiveIdentity(user.id, listId, sessionId, user.isGuest))
    }

    private fun setActiveList(user: SignedInUser, listId: Long) {
        preferences.edit().putLong(ACTIVE_LIST, listId).apply()
        val updated = user.copy(activeListId = listId)
        _currentUser.value = updated
        identityStore.set(ActiveIdentity(user.userId, listId, user.sessionId, user.isGuest))
    }

    private suspend fun selectFallbackList(user: SignedInUser) {
        val fallback = dao.firstActiveList(user.userId)?.id ?: createDefaultList(user.userId, now())
        setActiveList(user, fallback)
    }

    private fun clearLocalSession() {
        preferences.edit().remove(ACTIVE_TOKEN).remove(ACTIVE_LIST).apply()
        _currentUser.value = null
        identityStore.set(null)
    }

    private suspend fun audit(userId: Long?, action: String, type: String, subjectId: Long?, time: Long = now()) {
        dao.insertAudit(AuditEventEntity(userId = userId, action = action, subjectType = type, subjectId = subjectId, createdAt = time))
    }

    private companion object {
        const val ACTIVE_TOKEN = "active_session_token"
        const val ACTIVE_LIST = "active_list_id"
        const val MAX_FAILED_LOGINS = 5
        const val LOCK_DURATION_MS = 15 * 60 * 1000L
        const val SESSION_LIFETIME_MS = 30L * 24 * 60 * 60 * 1000
    }
}
