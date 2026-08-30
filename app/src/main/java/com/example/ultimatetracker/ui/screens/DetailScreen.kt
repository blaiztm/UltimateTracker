package com.example.ultimatetracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ultimatetracker.data.model.MediaType
import com.example.ultimatetracker.ui.components.MediaCover
import com.example.ultimatetracker.viewmodel.MediaViewModel

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun DetailScreen(viewModel: MediaViewModel, itemId: Long, onBack: () -> Unit, onEdit: () -> Unit) {
    val item by viewModel.observeItem(itemId).collectAsStateWithLifecycle(initialValue = null)
    var confirmDelete by remember { mutableStateOf(false) }
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(item?.title ?: "Произведение") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") } },
            actions = {
                IconButton(onClick = onEdit) { Icon(Icons.Outlined.Edit, "Редактировать") }
                IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Outlined.Delete, "Удалить") }
            },
        )
    }) { padding ->
        val value = item
        if (value == null) Text("Запись не найдена", Modifier.padding(padding).padding(24.dp)) else
            Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                MediaCover(value.coverUri, value.title, Modifier.fillMaxWidth().height(280.dp))
                Text(value.title, style = MaterialTheme.typography.headlineSmall)
                DetailRow("Тип", value.type.title)
                DetailRow("Категория", value.category.title)
                DetailRow(if (value.type == MediaType.MOVIE) "Длительность" else "Количество серий", if (value.type == MediaType.MOVIE) "${value.length} мин" else value.length.toString())
                DetailRow("Жанры", value.genres.ifEmpty { listOf("Не указаны") }.joinToString())
                DetailRow("Ключевые слова", value.keywords.ifEmpty { listOf("Не указаны") }.joinToString())
                Button(onClick = onEdit, modifier = Modifier.fillMaxWidth()) { Text("Редактировать") }
            }
    }
    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text("Удалить запись?") },
        text = { Text("Это действие нельзя отменить.") },
        confirmButton = { TextButton(onClick = { item?.let { viewModel.delete(it, onBack) } }) { Text("Удалить") } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Отмена") } },
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value)
    }
}
