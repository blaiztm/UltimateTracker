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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TravelExplore
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.BarChart
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.ultimatetracker.data.model.CategoryRef
import com.example.ultimatetracker.data.model.WatchCategory
import com.example.ultimatetracker.R
import com.example.ultimatetracker.ui.categoryLabel
import com.example.ultimatetracker.ui.categoryColor
import com.example.ultimatetracker.ui.mediaTypeLabel
import com.example.ultimatetracker.ui.components.MediaCover
import com.example.ultimatetracker.viewmodel.MediaViewModel
import com.example.ultimatetracker.viewmodel.SortMode
import com.example.ultimatetracker.viewmodel.SortDirection

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
fun HomeScreen(viewModel: MediaViewModel, onAdd: () -> Unit, onOpen: (Long) -> Unit, onSettings: () -> Unit, onStatistics: () -> Unit, onBrowse: (String) -> Unit, onAccount: () -> Unit) {
    val state by viewModel.homeState.collectAsStateWithLifecycle()
    var showFilters by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    LaunchedEffect(state.sortMode, state.sortDirection) { listState.scrollToItem(0) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { TextButton(onClick = { onBrowse("") }) { Icon(Icons.Outlined.TravelExplore, null); Spacer(Modifier.size(8.dp)); Text(stringResource(R.string.search_online)) } },
                actions = {
                    IconButton(onClick = onAccount) { Icon(Icons.Outlined.AccountCircle, stringResource(R.string.account_and_lists)) }
                    IconButton(onClick = onStatistics) { Icon(Icons.Outlined.BarChart, stringResource(R.string.statistics)) }
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
                    val categoryRef = CategoryRef.builtIn(category)
                    CategoryChip(categoryLabel(category), categoryRef, state.category == categoryRef) { viewModel.setCategory(categoryRef) }
                }
                items(state.categories, key = { category -> category.id }) { category ->
                    CategoryChip(category.name, category.id, state.category == category.id, categoryColor(category.id, state.categories)) { viewModel.setCategory(category.id) }
                }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SortMenu(state.sortMode, state.sortDirection, state.category?.let(CategoryRef::builtIn) == WatchCategory.PLANNED, viewModel::setSortMode, viewModel::toggleSortDirection, Modifier.weight(1f))
                if (state.availableGenres.isNotEmpty() || state.availableKeywords.isNotEmpty() || state.availableTypes.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { showFilters = true },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Outlined.Tune, null)
                        Spacer(Modifier.size(8.dp))
                        val count = state.selectedGenres.size + state.selectedKeywords.size + state.selectedTypes.size
                        Text(if (count == 0) stringResource(R.string.filters) else stringResource(R.string.filters_selected, count))
                    }
                }
            }
            if (state.items.isEmpty()) {
                Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text(if (state.query.isNotBlank() || state.category != null) stringResource(R.string.nothing_found) else stringResource(R.string.empty_collection), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.empty_collection_hint), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(state = listState, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(state.items, key = { it.id }) { item ->
                        MediaCard(item, state.categories, onClick = { onOpen(item.id) }, onCategory = { viewModel.updateCategory(item, it) })
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
        types = state.availableTypes,
        selectedTypes = state.selectedTypes,
        onGenre = viewModel::toggleGenre,
        onKeyword = viewModel::toggleKeyword,
        onType = viewModel::toggleType,
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
    types: List<String>,
    selectedTypes: Set<String>,
    onGenre: (String) -> Unit,
    onKeyword: (String) -> Unit,
    onType: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.filters)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (types.isNotEmpty()) {
                    Text(stringResource(R.string.type), style = MaterialTheme.typography.titleSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        types.forEach { type -> FilterChip(type in selectedTypes, { onType(type) }, { Text(mediaTypeLabel(type)) }) }
                    }
                }
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
private fun SortMenu(selected: SortMode, direction: SortDirection, planned: Boolean, onSelect: (SortMode) -> Unit, onToggleDirection: () -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    val modes = SortMode.entries.filter { if (planned) it != SortMode.RATING else it != SortMode.PRIORITY }
    Box(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.weight(1f)) { Text("${stringResource(R.string.sort)}: ${stringResource(selected.titleRes())}") }
            IconButton(onClick = onToggleDirection) {
                Icon(if (direction == SortDirection.ASCENDING) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward, stringResource(if (direction == SortDirection.ASCENDING) R.string.sort_ascending else R.string.sort_descending))
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            modes.forEach { mode ->
                DropdownMenuItem(text = { Text(stringResource(mode.titleRes())) }, onClick = { onSelect(mode); expanded = false })
            }
        }
    }
}

private fun SortMode.titleRes() = when (this) {
    SortMode.TITLE -> R.string.sort_title
    SortMode.RATING -> R.string.sort_rating
    SortMode.PRIORITY -> R.string.priority
    SortMode.DURATION -> R.string.sort_duration
}

@Composable
private fun CategoryChip(label: String, category: String?, selected: Boolean, customColor: androidx.compose.ui.graphics.Color? = null, onClick: () -> Unit) {
    val accent = customColor ?: category?.let(::categoryColor) ?: MaterialTheme.colorScheme.onSurfaceVariant
    AssistChip(
        onClick = onClick,
        label = { Text(label, color = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
        colors = AssistChipDefaults.assistChipColors(containerColor = if (selected) accent.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant),
    )
}

@Composable
private fun MediaCard(item: MediaItem, categories: List<com.example.ultimatetracker.data.local.CategoryEntity>, onClick: () -> Unit, onCategory: (String) -> Unit) {
    var categoryMenu by remember(item.id) { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            MediaCover(item.coverUri, item.title, Modifier.size(72.dp, 104.dp).clip(RoundedCornerShape(8.dp)))
            Column(Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(mediaTypeLabel(item.type), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Box {
                    TextButton(onClick = { categoryMenu = true }, contentPadding = PaddingValues(0.dp)) {
                        Text(CategoryRef.builtIn(item.category)?.let { categoryLabel(it) } ?: categories.firstOrNull { it.id == item.category }?.name ?: item.category, color = categoryColor(item.category, categories) ?: MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    DropdownMenu(expanded = categoryMenu, onDismissRequest = { categoryMenu = false }) {
                        WatchCategory.entries.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(categoryLabel(category)) },
                                onClick = { onCategory(CategoryRef.builtIn(category)); categoryMenu = false },
                            )
                        }
                        categories.forEach { category -> DropdownMenuItem(text = { Text(category.name) }, onClick = { onCategory(category.id); categoryMenu = false }) }
                    }
                }
                Text(stringResource(if (item.type == BuiltInMediaTypes.MOVIE) R.string.minutes_format else R.string.episodes_format, item.length))
                if (CategoryRef.builtIn(item.category) in setOf(WatchCategory.WATCHING, WatchCategory.ON_HOLD) && item.type != BuiltInMediaTypes.MOVIE) {
                    Text(stringResource(R.string.episodes_progress, item.watchedEpisodes, item.length))
                    LinearProgressIndicator(
                        progress = item.watchedEpisodes.toFloat() / item.length,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    )
                }
            }
            val score = if (CategoryRef.builtIn(item.category) == WatchCategory.PLANNED) item.priority else item.rating
            score?.let { score ->
                Text(
                    text = score.toString(),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (CategoryRef.builtIn(item.category) == WatchCategory.PLANNED) priorityColor(score) else if (score == 10) androidx.compose.ui.graphics.Color(0xFF00C9C8) else androidx.compose.ui.graphics.Color.hsv((score - 1) * 13.33f, 0.85f, 0.9f),
                )
            }
        }
    }
}

private fun priorityColor(priority: Int) = when (priority) {
    1 -> androidx.compose.ui.graphics.Color(0xFF2E7D32)
    2 -> androidx.compose.ui.graphics.Color(0xFF8BC34A)
    3 -> androidx.compose.ui.graphics.Color(0xFFFBC02D)
    else -> androidx.compose.ui.graphics.Color(0xFF757575)
}
