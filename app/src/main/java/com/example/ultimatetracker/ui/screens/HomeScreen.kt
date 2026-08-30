package com.example.ultimatetracker.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ultimatetracker.data.model.MediaItem
import com.example.ultimatetracker.data.model.WatchCategory
import com.example.ultimatetracker.ui.components.MediaCover
import com.example.ultimatetracker.viewmodel.MediaViewModel

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun HomeScreen(viewModel: MediaViewModel, onAdd: () -> Unit, onOpen: (Long) -> Unit) {
    val state by viewModel.homeState.collectAsStateWithLifecycle()
    Scaffold(
        topBar = { TopAppBar(title = { Text("UltimateTracker") }) },
        floatingActionButton = { FloatingActionButton(onClick = onAdd) { Icon(Icons.Default.Add, "Добавить") } },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::setQuery,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                placeholder = { Text("Название или ключевое слово") },
                leadingIcon = { Icon(Icons.Outlined.Search, null) },
                singleLine = true,
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item { CategoryChip("Все", state.category == null) { viewModel.setCategory(null) } }
                items(WatchCategory.entries) { category ->
                    CategoryChip(category.title, state.category == category) { viewModel.setCategory(category) }
                }
            }
            if (state.items.isEmpty()) {
                Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text(if (state.query.isNotBlank() || state.category != null) "Ничего не найдено" else "Коллекция пока пуста", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("Нажмите +, чтобы добавить первое произведение", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(state.items, key = { it.id }) { item -> MediaCard(item) { onOpen(item.id) } }
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    AssistChip(onClick = onClick, label = { Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) })
}

@Composable
private fun MediaCard(item: MediaItem, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            MediaCover(item.coverUri, item.title, Modifier.size(72.dp, 104.dp).clip(RoundedCornerShape(8.dp)))
            Column(Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(item.type.title, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(item.category.title, color = MaterialTheme.colorScheme.primary)
                Text(if (item.type.name == "MOVIE") "${item.length} мин" else "${item.length} серий")
            }
        }
    }
}
