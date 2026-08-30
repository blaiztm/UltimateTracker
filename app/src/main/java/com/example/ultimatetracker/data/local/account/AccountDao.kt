package com.example.ultimatetracker.data.local.account

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: UserEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertProfile(profile: UserProfileEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAccount(account: AccountEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSession(session: SessionEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertList(list: UserListEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAudit(event: AuditEventEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUserIfMissing(user: UserEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertProfileIfMissing(profile: UserProfileEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertListIfMissing(list: UserListEntity): Long

    @Update
    suspend fun updateProfile(profile: UserProfileEntity)

    @Update
    suspend fun updateAccount(account: AccountEntity)

    @Query("SELECT * FROM accounts WHERE emailNormalized = :email AND provider = 'LOCAL' AND deletedAt IS NULL LIMIT 1")
    suspend fun accountByEmail(email: String): AccountEntity?

    @Query("SELECT * FROM accounts WHERE userId = :userId AND deletedAt IS NULL ORDER BY createdAt LIMIT 1")
    suspend fun primaryAccount(userId: Long): AccountEntity?

    @Query("SELECT * FROM users WHERE id = :userId AND status = 'ACTIVE' AND deletedAt IS NULL LIMIT 1")
    suspend fun activeUser(userId: Long): UserEntity?

    @Query("SELECT * FROM user_profiles WHERE userId = :userId LIMIT 1")
    fun observeProfile(userId: Long): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profiles WHERE userId = :userId LIMIT 1")
    suspend fun profile(userId: Long): UserProfileEntity?

    @Query("SELECT * FROM accounts WHERE userId = :userId AND deletedAt IS NULL ORDER BY createdAt")
    fun observeAccounts(userId: Long): Flow<List<AccountEntity>>

    @Query("SELECT * FROM sessions WHERE tokenHash = :tokenHash AND revokedAt IS NULL AND expiresAt > :now LIMIT 1")
    suspend fun validSession(tokenHash: String, now: Long): SessionEntity?

    @Query("UPDATE sessions SET lastUsedAt = :now WHERE id = :sessionId AND revokedAt IS NULL")
    suspend fun touchSession(sessionId: Long, now: Long)

    @Query("UPDATE sessions SET revokedAt = :now WHERE id = :sessionId AND userId = :userId AND revokedAt IS NULL")
    suspend fun revokeSession(userId: Long, sessionId: Long, now: Long): Int

    @Query("UPDATE sessions SET revokedAt = :now WHERE userId = :userId AND revokedAt IS NULL")
    suspend fun revokeAllSessions(userId: Long, now: Long): Int

    @Query("SELECT * FROM sessions WHERE userId = :userId AND revokedAt IS NULL AND expiresAt > :now ORDER BY lastUsedAt DESC")
    fun observeSessions(userId: Long, now: Long): Flow<List<SessionEntity>>

    @Query("SELECT * FROM user_lists WHERE ownerUserId = :userId AND deletedAt IS NULL ORDER BY archivedAt IS NOT NULL, position, title COLLATE NOCASE")
    fun observeLists(userId: Long): Flow<List<UserListEntity>>

    @Query("SELECT * FROM user_lists WHERE ownerUserId = :userId AND deletedAt IS NULL ORDER BY position, id")
    suspend fun snapshotLists(userId: Long): List<UserListEntity>

    @Query("SELECT * FROM user_lists WHERE id = :listId AND ownerUserId = :userId AND deletedAt IS NULL LIMIT 1")
    suspend fun ownedList(userId: Long, listId: Long): UserListEntity?

    @Query("SELECT * FROM user_lists WHERE ownerUserId = :userId AND deletedAt IS NULL AND archivedAt IS NULL ORDER BY position, id LIMIT 1")
    suspend fun firstActiveList(userId: Long): UserListEntity?

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM user_lists WHERE ownerUserId = :userId AND deletedAt IS NULL")
    suspend fun nextListPosition(userId: Long): Int

    @Query("SELECT * FROM user_lists WHERE ownerUserId = :userId AND position = :position AND deletedAt IS NULL LIMIT 1")
    suspend fun listAtPosition(userId: Long, position: Int): UserListEntity?

    @Query("UPDATE user_lists SET position = :position, rowVersion = rowVersion + 1, updatedAt = :now WHERE id = :listId AND ownerUserId = :userId AND deletedAt IS NULL")
    suspend fun setListPosition(userId: Long, listId: Long, position: Int, now: Long): Int

    @Query("UPDATE user_lists SET title = :title, rowVersion = rowVersion + 1, updatedAt = :now WHERE id = :listId AND ownerUserId = :userId AND deletedAt IS NULL AND rowVersion = :expectedVersion")
    suspend fun renameList(userId: Long, listId: Long, title: String, expectedVersion: Long, now: Long): Int

    @Query("UPDATE user_lists SET archivedAt = :archivedAt, rowVersion = rowVersion + 1, updatedAt = :now WHERE id = :listId AND ownerUserId = :userId AND deletedAt IS NULL")
    suspend fun setListArchived(userId: Long, listId: Long, archivedAt: Long?, now: Long): Int

    @Query("UPDATE user_lists SET deletedAt = :now, rowVersion = rowVersion + 1, updatedAt = :now WHERE id = :listId AND ownerUserId = :userId AND deletedAt IS NULL")
    suspend fun softDeleteList(userId: Long, listId: Long, now: Long): Int

    @Query("UPDATE users SET status = 'DELETED', deletedAt = :now, updatedAt = :now WHERE id = :userId AND isGuest = 0 AND deletedAt IS NULL")
    suspend fun softDeleteUser(userId: Long, now: Long): Int

    @Query("UPDATE user_lists SET ownerUserId = :newUserId, updatedAt = :now, rowVersion = rowVersion + 1 WHERE ownerUserId = :guestUserId AND deletedAt IS NULL")
    suspend fun transferGuestLists(guestUserId: Long, newUserId: Long, now: Long): Int

    @Query("UPDATE accounts SET deletedAt = :now, passwordHash = NULL, passwordSalt = NULL, updatedAt = :now WHERE userId = :userId AND deletedAt IS NULL")
    suspend fun disableAccounts(userId: Long, now: Long)
}
