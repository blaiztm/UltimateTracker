package com.example.ultimatetracker.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ultimatetracker.data.model.MediaType
import com.example.ultimatetracker.data.model.WatchCategory
import com.example.ultimatetracker.ui.components.MediaCover
import com.example.ultimatetracker.viewmodel.MediaFormState
import com.example.ultimatetracker.viewmodel.MediaViewModel
import com.example.ultimatetracker.viewmodel.validationError

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun EditScreen(viewModel: MediaViewModel, itemId: Long, onBack: () -> Unit) {
    val existing by viewModel.observeItem(itemId).collectAsStateWithLifecycle(initialValue = null)
    var form by remember(itemId) { mutableStateOf(MediaFormState(id = itemId)) }
    var initialized by remember(itemId) { mutableStateOf(itemId == 0L) }
    var showErrors by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let {
            runCatching { context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            form = form.copy(coverUri = it.toString())
        }
    }
    LaunchedEffect(existing, initialized) {
        val item = existing
        if (!initialized && item != null) {
            form = MediaFormState(item.id, item.title, item.type, item.length.toString(), item.genres.joinToString(), item.keywords.joinToString(), item.category, item.coverUri, item.createdAt)
            initialized = true
        }
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(if (itemId == 0L) "Новое произведение" else "Редактирование") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") } },
        )
    }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MediaCover(form.coverUri, form.title, Modifier.fillMaxWidth().height(220.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, modifier = Modifier.weight(1f)) { Text("Выбрать обложку") }
                if (form.coverUri != null) OutlinedButton(onClick = { form = form.copy(coverUri = null) }) { Text("Убрать") }
            }
            OutlinedTextField(
                value = form.title,
                onValueChange = { form = form.copy(title = it) },
                label = { Text("Название *") },
                isError = showErrors && form.title.isBlank(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            EnumMenu("Тип", form.type, MediaType.entries, { it.title }) { form = form.copy(type = it) }
            OutlinedTextField(
                value = form.length,
                onValueChange = { value -> if (value.all(Char::isDigit)) form = form.copy(length = value) },
                label = { Text(if (form.type == MediaType.MOVIE) "Длительность, мин *" else "Количество серий *") },
                isError = showErrors && (form.length.toIntOrNull() ?: 0) <= 0,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(form.genres, { form = form.copy(genres = it) }, label = { Text("Жанры через запятую") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(form.keywords, { form = form.copy(keywords = it) }, label = { Text("Ключевые слова через запятую") }, modifier = Modifier.fillMaxWidth())
            EnumMenu("Категория", form.category, WatchCategory.entries, { it.title }) { form = form.copy(category = it) }
            if (showErrors) form.validationError()?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(
                onClick = {
                    showErrors = true
                    if (form.validationError() == null) viewModel.save(form, onBack)
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Сохранить") }
        }
    }
}

@Composable
private fun <T> EnumMenu(label: String, selected: T, options: List<T>, title: (T) -> String, onSelect: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text("$label: ${title(selected)}") }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(title(option)) }, onClick = { onSelect(option); expanded = false })
            }
        }
    }
}
