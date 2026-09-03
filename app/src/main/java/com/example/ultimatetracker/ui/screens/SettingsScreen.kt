package com.example.ultimatetracker.ui.screens

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.example.ultimatetracker.BuildConfig
import com.example.ultimatetracker.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ultimatetracker.viewmodel.MediaViewModel
import com.example.ultimatetracker.UltimateTrackerApplication
import com.example.ultimatetracker.ui.theme.AppTheme
import com.example.ultimatetracker.ui.theme.AppIconColor
import androidx.compose.ui.platform.LocalContext
import com.example.ultimatetracker.data.model.CategoryColor
import com.example.ultimatetracker.data.model.WatchCategory

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SettingsScreen(viewModel: MediaViewModel, onBack: () -> Unit) {
    val current = AppCompatDelegate.getApplicationLocales().get(0)?.language ?: "en"
    val savedToken by viewModel.tmdbToken.collectAsStateWithLifecycle()
    val app = LocalContext.current.applicationContext as UltimateTrackerApplication
    val theme by app.theme.collectAsStateWithLifecycle()
    val iconColor by app.iconColor.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    var token by remember(savedToken) { mutableStateOf(savedToken) }
    var section by remember { mutableStateOf(SettingsSection.HOME) }
    var categoryName by remember { mutableStateOf("") }
    var categoryColor by remember { mutableStateOf(CategoryColor.RED) }
    var deletingId by remember { mutableStateOf<String?>(null) }
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(stringResource(section.title)) },
            navigationIcon = { IconButton(onClick = { if (section == SettingsSection.HOME) onBack() else section = SettingsSection.HOME }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } },
        )
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            when (section) {
                SettingsSection.HOME -> {
                    SettingsSection.entries.filter { it != SettingsSection.HOME }.forEach { target ->
                        ListItem(
                            headlineContent = { Text(stringResource(target.title)) },
                            supportingContent = { Text(stringResource(target.summary)) },
                            modifier = Modifier.fillMaxWidth().clickable { section = target },
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    ListItem(headlineContent = { Text(stringResource(R.string.app_version)) }, supportingContent = { Text(BuildConfig.VERSION_NAME) })
                }
                SettingsSection.LANGUAGE -> {
                    LanguageRow(stringResource(R.string.english), current == "en") { setLanguage("en") }
                    LanguageRow(stringResource(R.string.russian), current == "ru") { setLanguage("ru") }
                }
                SettingsSection.APPEARANCE -> {
                    Text(stringResource(R.string.theme), modifier = Modifier.padding(16.dp))
                    AppTheme.entries.forEach { value -> ListItem(headlineContent = { Text(value.name.replace('_', ' ').lowercase().replaceFirstChar { it.titlecase() }) }, trailingContent = { RadioButton(selected = theme == value, onClick = { app.setTheme(value) }) }, modifier = Modifier.fillMaxWidth()) }
                    Text(stringResource(R.string.icon_color), modifier = Modifier.padding(16.dp))
                    AppIconColor.entries.forEach { value -> ListItem(headlineContent = { Text(value.name.replace('_', ' ').lowercase().replaceFirstChar { it.titlecase() }) }, trailingContent = { RadioButton(selected = iconColor == value, onClick = { app.setIconColor(value) }) }, modifier = Modifier.fillMaxWidth()) }
                }
                SettingsSection.TMDB -> {
                    OutlinedTextField(value = token, onValueChange = { token = it }, label = { Text(stringResource(R.string.tmdb_token)) }, visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
                    Button(onClick = { viewModel.saveTmdbToken(token) }, modifier = Modifier.fillMaxWidth().padding(16.dp)) { Text(stringResource(R.string.save_tmdb_token)) }
                }
                SettingsSection.CATEGORIES -> {
                    OutlinedTextField(value = categoryName, onValueChange = { categoryName = it }, label = { Text(stringResource(R.string.category_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
                    var colorsExpanded by remember { mutableStateOf(false) }
                    ListItem(headlineContent = { Text(stringResource(R.string.category_color)) }, supportingContent = { Text(categoryColor.name.lowercase().replaceFirstChar { it.titlecase() }) }, modifier = Modifier.fillMaxWidth().clickable { colorsExpanded = true })
                    DropdownMenu(expanded = colorsExpanded, onDismissRequest = { colorsExpanded = false }) { CategoryColor.entries.forEach { color -> DropdownMenuItem(text = { Text(color.name.lowercase().replaceFirstChar { it.titlecase() }) }, onClick = { categoryColor = color; colorsExpanded = false }) } }
                    Button(onClick = { if (categoryName.isNotBlank()) { viewModel.addCategory(categoryName, categoryColor); categoryName = "" } }, modifier = Modifier.fillMaxWidth().padding(16.dp)) { Text(stringResource(R.string.add_category)) }
                    categories.forEach { category -> ListItem(headlineContent = { Text(category.name) }, supportingContent = { Text(category.color.lowercase()) }, trailingContent = { TextButton(onClick = { deletingId = category.id }) { Text(stringResource(R.string.delete)) } }) }
                }
            }
        }
    }
    deletingId?.let { id ->
        val category = categories.firstOrNull { it.id == id } ?: return@let
        val replacement = WatchCategory.entries.first().name
        AlertDialog(onDismissRequest = { deletingId = null }, title = { Text(stringResource(R.string.delete_category)) }, text = { Text(stringResource(R.string.delete_category_message, category.name)) }, confirmButton = { TextButton(onClick = { viewModel.deleteCategory(id, replacement); deletingId = null }) { Text(stringResource(R.string.delete)) } }, dismissButton = { TextButton(onClick = { deletingId = null }) { Text(stringResource(R.string.cancel)) } })
    }
}

private enum class SettingsSection(val title: Int, val summary: Int) {
    HOME(R.string.settings, R.string.settings),
    APPEARANCE(R.string.appearance, R.string.appearance_summary),
    LANGUAGE(R.string.language, R.string.language_summary),
    TMDB(R.string.tmdb_settings, R.string.tmdb_summary),
    CATEGORIES(R.string.categories, R.string.categories_summary),
}

@Composable
private fun LanguageRow(title: String, selected: Boolean, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        trailingContent = { RadioButton(selected = selected, onClick = onClick) },
        modifier = Modifier.fillMaxWidth(),
    )
}

private fun setLanguage(tag: String) {
    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
}
