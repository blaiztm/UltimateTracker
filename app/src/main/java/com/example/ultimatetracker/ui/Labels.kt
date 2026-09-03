package com.example.ultimatetracker.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import com.example.ultimatetracker.data.local.CategoryEntity
import com.example.ultimatetracker.data.model.CategoryColor
import com.example.ultimatetracker.data.model.WatchCategory
import com.example.ultimatetracker.data.model.CategoryRef
import com.example.ultimatetracker.data.model.builtInTypeTitleRes

@Composable
fun mediaTypeLabel(type: String): String = builtInTypeTitleRes(type)?.let { stringResource(it) } ?: type

@Composable
fun categoryLabel(category: String): String = CategoryRef.builtIn(category)?.let { stringResource(it.titleRes) } ?: category

fun categoryColor(category: String): Color? = CategoryRef.builtIn(category)?.let {
    when (it) {
        WatchCategory.COMPLETED -> Color(0xFF83D6A0)
        WatchCategory.WATCHING -> Color(0xFFB99AFF)
        WatchCategory.ON_HOLD -> Color(0xFFFFC66D)
        WatchCategory.PLANNED -> Color(0xFF9DB7D9)
    }
}

@Composable
fun categoryColor(category: String, categories: List<CategoryEntity>): Color? {
    categoryColor(category)?.let { return it }
    return when (categories.firstOrNull { it.id == category }?.color?.let(CategoryColor::valueOf)) {
        CategoryColor.RED -> Color(0xFFE57373)
        CategoryColor.ORANGE -> Color(0xFFFFB74D)
        CategoryColor.YELLOW -> Color(0xFFFFF176)
        CategoryColor.GREEN -> Color(0xFF81C784)
        CategoryColor.BLUE -> Color(0xFF64B5F6)
        CategoryColor.PURPLE -> Color(0xFFBA68C8)
        CategoryColor.WHITE -> Color.White
        CategoryColor.BLACK -> Color.Black
        CategoryColor.RAINBOW -> Color.hsv(
            rememberInfiniteTransition(label = "rainbow").animateFloat(
                initialValue = 0f, targetValue = 360f,
                animationSpec = infiniteRepeatable(tween(2800)), label = "rainbowHue",
            ).value,
            0.55f,
            0.95f,
        )
        null -> null
    }
}

@Composable
fun categoryLabel(category: WatchCategory): String = stringResource(category.titleRes)

fun categoryColor(category: WatchCategory): Color = when (category) {
    WatchCategory.COMPLETED -> Color(0xFF83D6A0)
    WatchCategory.WATCHING -> Color(0xFFB99AFF)
    WatchCategory.ON_HOLD -> Color(0xFFFFC66D)
    WatchCategory.PLANNED -> Color(0xFF9DB7D9)
}
