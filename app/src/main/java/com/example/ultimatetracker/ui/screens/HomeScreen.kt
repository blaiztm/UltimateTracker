package com.example.ultimatetracker.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TravelExplore
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ultimatetracker.data.model.MediaItem
import com.example.ultimatetracker.data.model.BuiltInMediaTypes
import com.example.ultimatetracker.data.model.WatchCategory
import com.example.ultimatetracker.R
import com.example.ultimatetracker.ui.categoryLabel
import com.example.ultimatetracker.ui.categoryColor
import com.example.ultimatetracker.ui.mediaTypeLabel
import com.example.ultimatetracker.ui.components.MediaCover
import com.example.ultimatetracker.viewmodel.MediaViewModel

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
fun HomeScreen(viewModel: MediaViewModel, onAdd: () -> Unit, onOpen: (Long) -> Unit, onSettings: () -> Unit, onBrowse: () -> Unit, onAccount: () -> Unit) {
    val state by viewModel.homeState.collectAsStateWithLifecycle()
    var showFilters by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onBrowse) { Icon(Icons.Outlined.TravelExplore, stringResource(R.string.browse)) }
                    IconButton(onClick = onAccount) { Icon(Icons.Outlined.AccountCircle, stringResource(R.string.account_and_lists)) }
                    IconButton(onClick = onSettings) { Icon(Icons.Outlined.Settings, stringResource(R.string.settings)) }
                },
            )
        },
        floatingActionButton = { FloatingActionButton(onClick = onAdd) { Icon(Icons.Default.Add, stringResource(R.string.add)) } },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::setQuery,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                placeholder = { Text(stringResource(R.string.search_hint)) },
                leadingIcon = { Icon(Icons.Outlined.Search, null) },
                singleLine = true,
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item { CategoryChip(stringResource(R.string.all), null, state.category == null) { viewModel.setCategory(null) } }
                items(WatchCategory.entries) { category ->
                    CategoryChip(categoryLabel(category), category, state.category == category) { viewModel.setCategory(category) }
                }
            }
            if (state.availableGenres.isNotEmpty() || state.availableKeywords.isNotEmpty()) {
                OutlinedButton(
                    onClick = { showFilters = true },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                ) {
                    Icon(Icons.Outlined.Tune, null)
                    Spacer(Modifier.size(8.dp))
                    val count = state.selectedGenres.size + state.selectedKeywords.size
                    Text(if (count == 0) stringResource(R.string.filters) else stringResource(R.string.filters_selected, count))
                }
            }
            if (state.items.isEmpty()) {
                Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text(if (state.query.isNotBlank() || state.category != null) stringResource(R.string.nothing_found) else stringResource(R.string.empty_collection), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.empty_collection_hint), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(state.items, key = { it.id }) { item ->
                        MediaCard(item, onClick = { onOpen(item.id) }, onCategory = { viewModel.updateCategory(item, it) })
                    }
                }
            }
        }
    }
    if (showFilters) FilterDialog(
        genres = state.availableGenres,
        keywords = state.availableKeywords,
        selectedGenres = state.selectedGenres,
        selectedKeywords = state.selectedKeywords,
        onGenre = viewModel::toggleGenre,
        onKeyword = viewModel::toggleKeyword,
        onDismiss = { showFilters = false },
    )
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun FilterDialog(
    genres: List<String>,
    keywords: List<String>,
    selectedGenres: Set<String>,
    selectedKeywords: Set<String>,
    onGenre: (String) -> Unit,
    onKeyword: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.filters)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (genres.isNotEmpty()) {
                    Text(stringResource(R.string.genres), style = MaterialTheme.typography.titleSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        genres.forEach { genre ->
                            FilterChip(genre in selectedGenres, { onGenre(genre) }, { Text(genre) })
                        }
                    }
                }
                if (keywords.isNotEmpty()) {
                    Text(stringResource(R.string.keywords), style = MaterialTheme.typography.titleSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        keywords.forEach { keyword ->
                            FilterChip(keyword in selectedKeywords, { onKeyword(keyword) }, { Text(keyword) })
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.done)) } },
    )
}

@Composable
private fun CategoryChip(label: String, category: WatchCategory?, selected: Boolean, onClick: () -> Unit) {
    val accent = category?.let(::categoryColor) ?: MaterialTheme.colorScheme.onSurfaceVariant
    AssistChip(
        onClick = onClick,
        label = { Text(label, color = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
        colors = AssistChipDefaults.assistChipColors(containerColor = if (selected) accent.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant),
    )
}

@Composable
private fun MediaCard(item: MediaItem, onClick: () -> Unit, onCategory: (WatchCategory) -> Unit) {
    var categoryMenu by remember(item.id) { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            MediaCover(item.coverUri, item.title, Modifier.size(72.dp, 104.dp).clip(RoundedCornerShape(8.dp)))
            Column(Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(mediaTypeLabel(item.type), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Box {
                    TextButton(onClick = { categoryMenu = true }, contentPadding = PaddingValues(0.dp)) {
                        Text(categoryLabel(item.category), color = categoryColor(item.category))
                    }
                    DropdownMenu(expanded = categoryMenu, onDismissRequest = { categoryMenu = false }) {
                        WatchCategory.entries.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(categoryLabel(category)) },
                                onClick = { onCategory(category); categoryMenu = false },
                            )
                        }
                    }
                }
                Text(stringResource(if (item.type == BuiltInMediaTypes.MOVIE) R.string.minutes_format else R.string.episodes_format, item.length))
                if (item.category == WatchCategory.WATCHING && item.type != BuiltInMediaTypes.MOVIE) Text(stringResource(R.string.episodes_progress, item.watchedEpisodes, item.length))
            }
            item.rating?.let { rating ->
                Text(
                    text = rating.toString(),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (rating == 10) androidx.compose.ui.graphics.Color(0xFF00C9C8) else androidx.compose.ui.graphics.Color.hsv((rating - 1) * 13.33f, 0.85f, 0.9f),
                )
            }
        }
    }
}
