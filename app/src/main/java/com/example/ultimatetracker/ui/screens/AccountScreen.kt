package com.example.ultimatetracker.ui.screens

import android.net.Uri
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ultimatetracker.BuildConfig
import com.example.ultimatetracker.R
import com.example.ultimatetracker.data.backup.BackupFormatException
import com.example.ultimatetracker.data.local.account.UserListEntity
import com.example.ultimatetracker.viewmodel.AccountViewModel
import com.example.ultimatetracker.viewmodel.BackupUiStatus
import coil.compose.AsyncImage

private enum class ProfileSection { HOME, LISTS, ACCOUNT }

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AccountScreen(viewModel: AccountViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val user = state.user ?: return
    var section by remember { mutableStateOf(ProfileSection.HOME) }
    when (section) {
        ProfileSection.HOME -> ProfileHome(user.displayName, user.email, user.avatarUri, onBack, { section = ProfileSection.LISTS }, { section = ProfileSection.ACCOUNT }, viewModel::logout)
        ProfileSection.LISTS -> ListsSection(viewModel) { section = ProfileSection.HOME }
        ProfileSection.ACCOUNT -> AccountSettingsSection(viewModel, user.displayName, user.isGuest) { section = ProfileSection.HOME }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ProfileHome(userName: String, email: String?, avatarUri: String?, onBack: () -> Unit, onLists: () -> Unit, onAccount: () -> Unit, onSignOut: () -> Unit) {
    Scaffold(topBar = {
        TopAppBar(title = { Text(stringResource(R.string.profile)) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(Modifier.padding(bottom = 10.dp)) {
                ProfileAvatar(avatarUri, 72)
                Text(userName.ifBlank { stringResource(R.string.guest) }, style = MaterialTheme.typography.headlineSmall)
                Text(email ?: stringResource(R.string.local_guest), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            ProfileFolder(Icons.Outlined.Folder, stringResource(R.string.lists_folder), stringResource(R.string.lists_folder_hint), onLists)
            ProfileFolder(Icons.Outlined.AccountCircle, stringResource(R.string.account_settings), stringResource(R.string.account_settings_hint), onAccount)
            Column(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.Bottom) {
                OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.sign_out)) }
            }
        }
    }
}

@Composable
private fun ProfileFolder(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(icon, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.titleMedium); Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Text("›", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ListsSection(viewModel: AccountViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val user = state.user ?: return
    var newList by remember { mutableStateOf("") }
    var editList by remember { mutableStateOf<UserListEntity?>(null) }
    var editTitle by remember { mutableStateOf("") }
    var pendingListDelete by remember { mutableStateOf<UserListEntity?>(null) }
    var pendingImport by remember { mutableStateOf<Uri?>(null) }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> uri?.let { viewModel.exportBackup(it, BuildConfig.VERSION_NAME) } }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> pendingImport = uri }
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.lists_folder)) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text(stringResource(R.string.your_lists), style = MaterialTheme.typography.titleLarge) }
            items(state.lists, key = { it.id }) { list ->
                Card(onClick = { if (list.archivedAt == null) viewModel.selectList(list.id) }, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(if (list.id == user.activeListId) "✓ ${list.title}" else list.title, style = MaterialTheme.typography.titleMedium)
                        if (list.archivedAt != null) Text(stringResource(R.string.archived))
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            TextButton(onClick = { viewModel.moveList(list, -1) }) { Text("↑") }
                            TextButton(onClick = { viewModel.moveList(list, 1) }) { Text("↓") }
                            TextButton(onClick = { editList = list; editTitle = list.title }) { Text(stringResource(R.string.rename)) }
                            TextButton(onClick = { viewModel.archiveList(list, list.archivedAt == null) }) { Text(stringResource(if (list.archivedAt == null) R.string.archive else R.string.restore)) }
                            IconButton(onClick = { pendingListDelete = list }) { Icon(Icons.Default.Delete, stringResource(R.string.delete_list), tint = MaterialTheme.colorScheme.error) }
                        }
                    }
                }
            }
            item {
                OutlinedTextField(newList, { newList = it }, label = { Text(stringResource(R.string.new_list_name)) }, modifier = Modifier.fillMaxWidth())
                Button(onClick = { viewModel.createList(newList); newList = "" }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.create_list)) }
            }
            item {
                Text(stringResource(R.string.backup_transfer), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.backup_transfer_hint), color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedButton(onClick = { exportLauncher.launch("ultimate-tracker-${BuildConfig.VERSION_NAME}.utracker.json") }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.FileUpload, null); Text(stringResource(R.string.export_lists), modifier = Modifier.padding(start = 8.dp)) }
                OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.FileDownload, null); Text(stringResource(R.string.import_lists), modifier = Modifier.padding(start = 8.dp)) }
                when (val backup = state.backupStatus) {
                    BackupUiStatus.Idle -> Unit
                    is BackupUiStatus.Exported -> Text(stringResource(R.string.export_success, backup.result.listCount, backup.result.itemCount), color = MaterialTheme.colorScheme.primary)
                    is BackupUiStatus.Imported -> Text(stringResource(R.string.import_success, backup.result.listCount, backup.result.itemCount), color = MaterialTheme.colorScheme.primary)
                    is BackupUiStatus.Failed -> Text(backupError(backup.reason), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
    editList?.let { list -> AlertDialog(onDismissRequest = { editList = null }, title = { Text(stringResource(R.string.rename_list)) }, text = { OutlinedTextField(editTitle, { editTitle = it }, singleLine = true) }, confirmButton = { TextButton(onClick = { viewModel.renameList(list, editTitle); editList = null }) { Text(stringResource(R.string.save)) } }, dismissButton = { TextButton(onClick = { editList = null }) { Text(stringResource(R.string.cancel)) } }) }
    pendingListDelete?.let { list -> AlertDialog(onDismissRequest = { pendingListDelete = null }, title = { Text(stringResource(R.string.delete_list)) }, text = { Text(stringResource(R.string.delete_list_warning, list.title)) }, confirmButton = { TextButton(onClick = { pendingListDelete = null; viewModel.deleteList(list) }) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) } }, dismissButton = { TextButton(onClick = { pendingListDelete = null }) { Text(stringResource(R.string.cancel)) } }) }
    pendingImport?.let { uri -> AlertDialog(onDismissRequest = { pendingImport = null }, title = { Text(stringResource(R.string.import_lists)) }, text = { Text(stringResource(R.string.import_warning)) }, confirmButton = { TextButton(onClick = { pendingImport = null; viewModel.importBackup(uri) }) { Text(stringResource(R.string.import_action)) } }, dismissButton = { TextButton(onClick = { pendingImport = null }) { Text(stringResource(R.string.cancel)) } }) }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AccountSettingsSection(viewModel: AccountViewModel, currentName: String, isGuest: Boolean, onBack: () -> Unit) {
    var name by remember(currentName) { mutableStateOf(currentName) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    var avatarUri by remember(state.user?.avatarUri) { mutableStateOf(state.user?.avatarUri) }
    var confirmDelete by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            runCatching { context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            avatarUri = it.toString()
        }
    }
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.account_settings)) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ProfileAvatar(avatarUri, 104)
            OutlinedButton(onClick = { avatarPicker.launch(arrayOf("image/*")) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.choose_profile_picture)) }
            OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.display_name)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Button(onClick = { viewModel.updateProfile(name, avatarUri = avatarUri) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.save_profile)) }
            if (!isGuest) TextButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.delete_account), color = MaterialTheme.colorScheme.error) }
        }
    }
    if (confirmDelete) AlertDialog(onDismissRequest = { confirmDelete = false }, title = { Text(stringResource(R.string.delete_account)) }, text = { Text(stringResource(R.string.delete_account_warning)) }, confirmButton = { TextButton(onClick = { confirmDelete = false; viewModel.deleteAccount() }) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) } }, dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } })
}

@Composable
private fun backupError(reason: BackupFormatException.Reason?): String = stringResource(when (reason) {
    BackupFormatException.Reason.WRONG_FORMAT -> R.string.backup_wrong_format
    BackupFormatException.Reason.UNSUPPORTED_VERSION -> R.string.backup_newer_version
    BackupFormatException.Reason.LIMIT_EXCEEDED -> R.string.backup_too_large
    BackupFormatException.Reason.MALFORMED, BackupFormatException.Reason.INVALID_DATA -> R.string.backup_invalid
    null -> R.string.backup_io_error
})

@Composable
private fun ProfileAvatar(uri: String?, size: Int) {
    val fallback = rememberVectorPainter(Icons.Outlined.AccountCircle)
    AsyncImage(
        model = uri,
        contentDescription = stringResource(R.string.profile_picture),
        placeholder = fallback,
        error = fallback,
        fallback = fallback,
        modifier = Modifier.size(size.dp).clip(CircleShape),
    )
}
