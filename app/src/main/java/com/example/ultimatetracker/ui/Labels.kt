package com.example.ultimatetracker.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Color
import com.example.ultimatetracker.data.model.WatchCategory
import com.example.ultimatetracker.data.model.builtInTypeTitleRes

@Composable
fun mediaTypeLabel(type: String): String = builtInTypeTitleRes(type)?.let { stringResource(it) } ?: type

@Composable
fun categoryLabel(category: WatchCategory): String = stringResource(category.titleRes)

fun categoryColor(category: WatchCategory): Color = when (category) {
    WatchCategory.COMPLETED -> Color(0xFF83D6A0)
    WatchCategory.WATCHING -> Color(0xFFB99AFF)
    WatchCategory.ON_HOLD -> Color(0xFFFFC66D)
    WatchCategory.PLANNED -> Color(0xFF9DB7D9)
}
