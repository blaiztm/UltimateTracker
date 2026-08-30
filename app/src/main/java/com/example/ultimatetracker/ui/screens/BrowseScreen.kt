package com.example.ultimatetracker.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ultimatetracker.R
import com.example.ultimatetracker.data.remote.CatalogItem
import com.example.ultimatetracker.ui.components.MediaCover
import com.example.ultimatetracker.ui.mediaTypeLabel
import com.example.ultimatetracker.viewmodel.MediaViewModel

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun BrowseScreen(viewModel: MediaViewModel, onBack: () -> Unit, onUse: (CatalogItem) -> Unit) {
    val state by viewModel.catalogState.collectAsStateWithLifecycle()
    val language = LocalConfiguration.current.locales[0].language
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(stringResource(R.string.online_search)) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } },
        )
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = state.query,
                onValueChange = { viewModel.searchCatalog(it, language) },
                placeholder = { Text(stringResource(R.string.search_catalog_hint)) },
                leadingIcon = { Icon(Icons.Outlined.Search, null) },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                singleLine = true,
            )
            when {
                state.keyMissing -> Message(stringResource(R.string.tmdb_key_missing))
                state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
                state.hasError -> Message(stringResource(R.string.network_error))
                else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(state.results, key = { "${it.mediaType}-${it.id}" }) { item ->
                        CatalogCard(item) {
                            viewModel.selectCatalogItem(item, language) { onUse(item) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Message(text: String) {
    Text(text, modifier = Modifier.padding(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun CatalogCard(item: CatalogItem, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            MediaCover(item.coverUri, item.title, Modifier.size(64.dp, 92.dp).clip(RoundedCornerShape(6.dp)))
            Column(Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.titleMedium)
                if (item.originalTitle != item.title) Text(item.originalTitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(listOfNotNull(mediaTypeLabel(item.mediaType), item.year).joinToString(" • "))
                Text(stringResource(R.string.use_title), color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
