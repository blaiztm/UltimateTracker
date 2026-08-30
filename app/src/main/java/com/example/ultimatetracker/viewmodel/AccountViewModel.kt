package com.example.ultimatetracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ultimatetracker.data.local.account.UserListEntity
import com.example.ultimatetracker.data.repository.AccountRepository
import com.example.ultimatetracker.data.repository.AccountResult
import com.example.ultimatetracker.data.repository.SignedInUser
import com.example.ultimatetracker.data.repository.BackupRepository
import com.example.ultimatetracker.data.repository.BackupResult
import com.example.ultimatetracker.data.backup.BackupFormatException
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AccountUiState(
    val initialized: Boolean = false,
    val user: SignedInUser? = null,
    val lists: List<UserListEntity> = emptyList(),
    val error: AccountResult? = null,
    val busy: Boolean = false,
    val backupStatus: BackupUiStatus = BackupUiStatus.Idle,
)

sealed interface BackupUiStatus {
    data object Idle : BackupUiStatus
    data class Exported(val result: BackupResult) : BackupUiStatus
    data class Imported(val result: BackupResult) : BackupUiStatus
    data class Failed(val reason: BackupFormatException.Reason?) : BackupUiStatus
}

class AccountViewModel(
    private val repository: AccountRepository,
    private val backupRepository: BackupRepository,
) : ViewModel() {
    private val initialized = MutableStateFlow(false)
    private val error = MutableStateFlow<AccountResult?>(null)
    private val busy = MutableStateFlow(false)
    private val backupStatus = MutableStateFlow<BackupUiStatus>(BackupUiStatus.Idle)

    private val accountState = combine(
        initialized,
        repository.currentUser,
        repository.lists,
        error,
        busy,
    ) { ready, user, lists, issue, working ->
        AccountUiState(ready, user, lists, issue, working)
    }
    val state: StateFlow<AccountUiState> = combine(accountState, backupStatus) { account, backup ->
        account.copy(backupStatus = backup)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AccountUiState())

    init {
        viewModelScope.launch {
            repository.initialize()
            if (repository.currentUser.value == null) {
                repository.continueAsGuest()
            }
            initialized.value = true
        }
    }

    fun continueAsGuest() = run { repository.continueAsGuest() }
    fun register(email: String, password: String, name: String) = run { repository.register(email, password, name) }
    fun login(email: String, password: String) = run { repository.login(email, password) }
    fun logout() = run { repository.logout() }
    fun logoutAll() = run { repository.logoutAll() }
    fun updateProfile(name: String, locale: String = "", avatarUri: String? = null) = run { repository.updateProfile(name, locale, avatarUri) }
    fun createList(title: String) = run { repository.createList(title) }
    fun selectList(id: Long) = run { repository.selectList(id) }
    fun renameList(list: UserListEntity, title: String) = run { repository.renameList(list.id, title, list.rowVersion) }
    fun moveList(list: UserListEntity, direction: Int) = run { repository.moveList(list.id, direction) }
    fun archiveList(list: UserListEntity, archived: Boolean) = run { repository.archiveList(list.id, archived) }
    fun deleteList(list: UserListEntity) = run { repository.deleteList(list.id) }
    fun deleteAccount() = run { repository.deleteAccount() }
    fun clearError() { error.value = null }
    fun clearBackupStatus() { backupStatus.value = BackupUiStatus.Idle }

    fun exportBackup(uri: Uri, appVersion: String) = runBackup {
        backupStatus.value = BackupUiStatus.Exported(backupRepository.exportTo(uri, appVersion))
    }

    fun importBackup(uri: Uri) = runBackup {
        backupStatus.value = BackupUiStatus.Imported(backupRepository.importFrom(uri))
    }

    private fun run(action: suspend () -> AccountResult) {
        if (busy.value) return
        viewModelScope.launch {
            busy.value = true
            val result = action()
            error.value = result.takeUnless { it == AccountResult.Success }
            busy.value = false
        }
    }

    private fun runBackup(action: suspend () -> Unit) {
        if (busy.value) return
        viewModelScope.launch {
            busy.value = true
            backupStatus.value = BackupUiStatus.Idle
            try {
                action()
            } catch (error: BackupFormatException) {
                backupStatus.value = BackupUiStatus.Failed(error.reason)
            } catch (_: Exception) {
                backupStatus.value = BackupUiStatus.Failed(null)
            } finally {
                busy.value = false
            }
        }
    }
}

class AccountViewModelFactory(
    private val repository: AccountRepository,
    private val backupRepository: BackupRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = AccountViewModel(repository, backupRepository) as T
}
