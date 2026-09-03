package com.example.ultimatetracker.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ultimatetracker.R
import com.example.ultimatetracker.data.model.CategoryRef
import com.example.ultimatetracker.data.model.WatchCategory
import com.example.ultimatetracker.ui.categoryColor
import com.example.ultimatetracker.ui.categoryLabel
import com.example.ultimatetracker.ui.mediaTypeLabel
import com.example.ultimatetracker.viewmodel.MediaViewModel
import com.example.ultimatetracker.viewmodel.StatisticsUiState
import java.util.Locale

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun StatisticsScreen(viewModel: MediaViewModel, onBack: () -> Unit) {
    val state by viewModel.statistics.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val statusOverrides by viewModel.statusOverrides.collectAsStateWithLifecycle()
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(stringResource(R.string.statistics)) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } },
        )
    }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            if (state.totalTitles == 0) Text(stringResource(R.string.statistics_empty), modifier = Modifier.padding(top = 24.dp)) else {
                JourneyHero(state)
                StatusOverview(state, categories, statusOverrides)
                KeyMetrics(state)
                HistogramSection(stringResource(R.string.favorite_genres), state.genres)
                HistogramSection(stringResource(R.string.work_types), state.typeCounts.entries.map { it.key to it.value }, label = { mediaTypeLabel(it) })
                KeywordsSection(state.keywords)
                HistogramSection(stringResource(R.string.rating_distribution), state.ratingDistribution.entries.sortedByDescending { it.key }.map { "${it.key} / 10" to it.value })
                HistogramSection(stringResource(R.string.planned_priorities), state.priorityDistribution.entries.sortedBy { it.key }.map { stringResource(R.string.priority_value, it.key) to it.value })
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun JourneyHero(state: StatisticsUiState) {
    val progress = state.completedCount.toFloat() / state.totalTitles
    val animatedProgress by loadAnimation(progress)
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.your_media_journey), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(20.dp))
            Box(Modifier.size(172.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(progress = { 1f }, modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = .12f), strokeWidth = 13.dp)
                CircularProgressIndicator(progress = { animatedProgress }, modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.primary, strokeWidth = 13.dp, trackColor = Color.Transparent)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.completed), style = MaterialTheme.typography.labelLarge)
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.completed_titles_count, state.completedCount, state.totalTitles), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun StatusOverview(state: StatisticsUiState, categories: List<com.example.ultimatetracker.data.local.CategoryEntity>, overrides: List<com.example.ultimatetracker.data.local.StatusOverrideEntity>) {
    SectionTitle(stringResource(R.string.collection_status))
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        state.categoryCounts.entries.sortedWith(compareByDescending<Map.Entry<String, Int>> { CategoryRef.builtIn(it.key) == WatchCategory.COMPLETED }.thenByDescending { it.value }).forEach { (category, count) ->
            val builtIn = CategoryRef.builtIn(category)
            val label = builtIn?.let { categoryLabel(it, overrides) } ?: categories.firstOrNull { it.id == category }?.name ?: humanize(category)
            val color = categoryColor(category, categories, overrides) ?: MaterialTheme.colorScheme.secondary
            ProportionalRow(label, count, count.toFloat() / state.totalTitles, color, emphasize = builtIn == WatchCategory.COMPLETED, showPercent = true)
        }
    }
}

@Composable
private fun KeyMetrics(state: StatisticsUiState) {
    SectionTitle(stringResource(R.string.key_metrics))
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Metric(stringResource(R.string.average_rating), state.averageRating?.let { String.format(Locale.getDefault(), "%.1f / 10", it) } ?: stringResource(R.string.not_specified), Modifier.weight(1f))
            Metric(stringResource(R.string.movie_duration), formatDuration(state.movieMinutes), Modifier.weight(1f))
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        val progress = if (state.totalEpisodes == 0) 0f else state.watchedEpisodes.toFloat() / state.totalEpisodes
        val remaining = (state.totalEpisodes - state.watchedEpisodes).coerceAtLeast(0)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.episode_progress), style = MaterialTheme.typography.titleSmall)
                Text("${state.watchedEpisodes} / ${state.totalEpisodes}", fontWeight = FontWeight.SemiBold)
            }
            AnimatedBar(progress, MaterialTheme.colorScheme.primary)
            Text(stringResource(R.string.episodes_remaining, remaining), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Metric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun HistogramSection(title: String, values: List<Pair<String, Int>>, label: @Composable (String) -> String = { humanize(it) }) {
    if (values.isEmpty()) return
    SectionTitle(title)
    val sorted = values.sortedByDescending { it.second }
    val maximum = sorted.maxOf { it.second }.coerceAtLeast(1)
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        sorted.forEach { (name, count) -> ProportionalRow(label(name), count, count.toFloat() / maximum, MaterialTheme.colorScheme.primary) }
    }
}

@Composable
private fun KeywordsSection(keywords: List<Pair<String, Int>>) {
    if (keywords.isEmpty()) return
    SectionTitle(stringResource(R.string.keywords))
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        keywords.sortedByDescending { it.second }.forEach { (keyword, count) ->
            Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceVariant) {
                Text("$keyword  $count", Modifier.padding(horizontal = 14.dp, vertical = 9.dp), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun ProportionalRow(label: String, count: Int, fraction: Float, color: Color, emphasize: Boolean = false, showPercent: Boolean = false) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, fontWeight = if (emphasize) FontWeight.Bold else FontWeight.Medium)
            Text(if (showPercent) "$count  ·  ${(fraction * 100).toInt()}%" else count.toString(), style = MaterialTheme.typography.labelLarge, color = if (emphasize) color else MaterialTheme.colorScheme.onSurfaceVariant)
        }
        AnimatedBar(fraction, color, if (emphasize) 10.dp else 7.dp)
    }
}

@Composable
private fun AnimatedBar(fraction: Float, color: Color, height: Dp = 8.dp) {
    val animatedFraction by loadAnimation(fraction.coerceIn(0f, 1f))
    Box(Modifier.fillMaxWidth().height(height).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)) {
        if (animatedFraction > 0f) Box(Modifier.fillMaxWidth(animatedFraction).height(height).clip(CircleShape).background(color))
    }
}

@Composable
private fun loadAnimation(target: Float): State<Float> {
    var loaded by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { loaded = true }
    return animateFloatAsState(if (loaded) target else 0f, tween(750), label = "statisticsLoad")
}

@Composable
private fun SectionTitle(title: String) = Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 2.dp))

private fun humanize(value: String): String = value.lowercase().replace('_', ' ').replaceFirstChar { it.titlecase() }

@Composable
private fun formatDuration(minutes: Int): String {
    val hours = minutes / 60
    val remainder = minutes % 60
    return when {
        hours > 0 && remainder > 0 -> stringResource(R.string.hours_minutes_format, hours, remainder)
        hours > 0 -> stringResource(R.string.hours_format, hours)
        else -> stringResource(R.string.minutes_short_format, remainder)
    }
}
