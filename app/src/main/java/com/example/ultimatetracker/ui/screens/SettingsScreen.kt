package com.example.ultimatetracker.ui.screens

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SettingsScreen(viewModel: MediaViewModel, onBack: () -> Unit) {
    val current = AppCompatDelegate.getApplicationLocales().get(0)?.language ?: "en"
    val savedToken by viewModel.tmdbToken.collectAsStateWithLifecycle()
    var token by remember(savedToken) { mutableStateOf(savedToken) }
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(stringResource(R.string.settings)) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } },
        )
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Text(stringResource(R.string.language), modifier = Modifier.padding(16.dp))
            LanguageRow(stringResource(R.string.english), current == "en") { setLanguage("en") }
            LanguageRow(stringResource(R.string.russian), current == "ru") { setLanguage("ru") }
            Text(stringResource(R.string.tmdb_settings), modifier = Modifier.padding(16.dp))
            OutlinedTextField(
                value = token,
                onValueChange = { token = it },
                label = { Text(stringResource(R.string.tmdb_token)) },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )
            Button(
                onClick = { viewModel.saveTmdbToken(token) },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            ) { Text(stringResource(R.string.save_tmdb_token)) }
            ListItem(
                headlineContent = { Text(stringResource(R.string.app_version)) },
                supportingContent = { Text(BuildConfig.VERSION_NAME) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
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
