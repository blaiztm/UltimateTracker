package com.example.ultimatetracker.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ultimatetracker.R
import com.example.ultimatetracker.data.model.CategoryRef
import com.example.ultimatetracker.viewmodel.MediaViewModel

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun StatisticsScreen(viewModel: MediaViewModel, onBack: () -> Unit) {
    val state by viewModel.statistics.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.statistics)) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)) {
            if (state.totalTitles == 0) Text(stringResource(R.string.statistics_empty)) else {
                ListItem(headlineContent = { Text(stringResource(R.string.total_titles)) }, trailingContent = { Text(state.totalTitles.toString()) })
                ListItem(headlineContent = { Text(stringResource(R.string.completed)) }, trailingContent = { Text("${state.completedCount} (${state.completedCount * 100 / state.totalTitles}%)") })
                StatSection(stringResource(R.string.category), state.categoryCounts.mapKeys { entry -> CategoryRef.builtIn(entry.key)?.name ?: categories.firstOrNull { it.id == entry.key }?.name ?: entry.key })
                StatSection(stringResource(R.string.type), state.typeCounts)
                ListItem(headlineContent = { Text(stringResource(R.string.average_rating)) }, trailingContent = { Text(state.averageRating?.let { "%.1f".format(it) } ?: stringResource(R.string.not_specified)) })
                ListItem(headlineContent = { Text(stringResource(R.string.movie_minutes)) }, trailingContent = { Text(state.movieMinutes.toString()) })
                ListItem(headlineContent = { Text(stringResource(R.string.watched_episodes)) }, trailingContent = { Text("${state.watchedEpisodes}/${state.totalEpisodes}") })
                StatSection(stringResource(R.string.priority), state.priorityDistribution.mapKeys { it.key.toString() })
                StatSection(stringResource(R.string.genres), state.genres.toMap())
                StatSection(stringResource(R.string.keywords), state.keywords.toMap())
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable private fun StatSection(title: String, values: Map<String, Int>) { if (values.isNotEmpty()) { Text(title); values.forEach { (name, count) -> ListItem(headlineContent = { Text(name) }, trailingContent = { Text(count.toString()) }) } } }
