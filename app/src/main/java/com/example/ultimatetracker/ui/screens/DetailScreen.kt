package com.example.ultimatetracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ultimatetracker.data.model.BuiltInMediaTypes
import com.example.ultimatetracker.data.model.WatchCategory
import com.example.ultimatetracker.data.model.CategoryRef
import com.example.ultimatetracker.ui.components.MediaCover
import com.example.ultimatetracker.viewmodel.MediaViewModel
import com.example.ultimatetracker.R
import com.example.ultimatetracker.ui.categoryLabel
import com.example.ultimatetracker.ui.categoryColor
import com.example.ultimatetracker.ui.mediaTypeLabel
import java.text.DateFormat

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun DetailScreen(viewModel: MediaViewModel, itemId: Long, onBack: () -> Unit, onEdit: () -> Unit) {
    val item by viewModel.observeItem(itemId).collectAsStateWithLifecycle(initialValue = null)
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(item?.title ?: stringResource(R.string.media_item)) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } },
            actions = {
                IconButton(onClick = onEdit) { Icon(Icons.Outlined.Edit, stringResource(R.string.edit)) }
                IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Outlined.Delete, stringResource(R.string.delete)) }
            },
        )
    }) { padding ->
        val value = item
        if (value == null) Text(stringResource(R.string.item_not_found), Modifier.padding(padding).padding(24.dp)) else
            Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                MediaCover(value.coverUri, value.title, Modifier.fillMaxWidth().height(280.dp))
                Text(value.title, style = MaterialTheme.typography.headlineSmall)
                DetailRow(stringResource(R.string.type), mediaTypeLabel(value.type))
                DetailRow(stringResource(R.string.category), CategoryRef.builtIn(value.category)?.let { categoryLabel(it) } ?: categories.firstOrNull { it.id == value.category }?.name ?: value.category, categoryColor(value.category, categories))
                DetailRow(if (value.type == BuiltInMediaTypes.MOVIE) stringResource(R.string.duration) else stringResource(R.string.episode_count), stringResource(if (value.type == BuiltInMediaTypes.MOVIE) R.string.minutes_format else R.string.episodes_format, value.length))
                DetailRow(stringResource(R.string.genres), value.genres.ifEmpty { listOf(stringResource(R.string.not_specified)) }.joinToString())
                DetailRow(stringResource(R.string.keywords), value.keywords.ifEmpty { listOf(stringResource(R.string.not_specified)) }.joinToString())
                val categoryEnum = CategoryRef.builtIn(value.category)
                if (categoryEnum == WatchCategory.PLANNED) value.priority?.let { DetailRow(stringResource(R.string.priority), it.toString(), priorityColor(it)) }
                else value.rating?.let { rating -> DetailRow(stringResource(R.string.rating), stringResource(R.string.rating_format, rating)) }
                if (categoryEnum in setOf(WatchCategory.WATCHING, WatchCategory.ON_HOLD) && value.type != BuiltInMediaTypes.MOVIE) DetailRow(stringResource(R.string.watched_episodes), stringResource(R.string.episodes_progress, value.watchedEpisodes, value.length))
                value.watchStartedAt?.let { DetailRow(stringResource(R.string.watch_start_date), DateFormat.getDateInstance().format(it)) }
                value.watchEndedAt?.let { DetailRow(stringResource(R.string.watch_end_date), DateFormat.getDateInstance().format(it)) }
                if (value.review.isNotBlank()) {
                    Text(stringResource(R.string.review), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(value.review, style = MaterialTheme.typography.bodyLarge)
                }
                Button(onClick = onEdit, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.edit)) }
            }
    }
    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text(stringResource(R.string.delete_item_title)) },
        text = { Text(stringResource(R.string.delete_item_message)) },
        confirmButton = { TextButton(onClick = { item?.let { viewModel.delete(it, onBack) } }) { Text(stringResource(R.string.delete)) } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } },
    )
}

private fun priorityColor(priority: Int) = when (priority) {
    1 -> androidx.compose.ui.graphics.Color(0xFF2E7D32)
    2 -> androidx.compose.ui.graphics.Color(0xFF8BC34A)
    3 -> androidx.compose.ui.graphics.Color(0xFFFBC02D)
    else -> androidx.compose.ui.graphics.Color(0xFF757575)
}

@Composable
private fun DetailRow(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color? = null) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, color = valueColor ?: MaterialTheme.colorScheme.onSurface)
    }
}
