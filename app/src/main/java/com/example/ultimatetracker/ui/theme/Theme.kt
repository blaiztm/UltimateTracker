package com.example.ultimatetracker.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val UTrackerPurple = Color(0xFFB99AFF)
private val UTrackerPurpleBright = Color(0xFFD8C7FF)
private val UTrackerDeep = Color(0xFF120E1C)
private val UTrackerSurface = Color(0xFF1B1529)
private val UTrackerSurfaceHigh = Color(0xFF2A2140)
private val UTrackerOutline = Color(0xFF675A7F)

private val UTrackerDarkColors: ColorScheme = darkColorScheme(
    primary = UTrackerPurple,
    onPrimary = Color(0xFF26144A),
    primaryContainer = Color(0xFF4B3574),
    onPrimaryContainer = UTrackerPurpleBright,
    secondary = Color(0xFFD0B8F5),
    onSecondary = Color(0xFF302046),
    secondaryContainer = Color(0xFF44345D),
    onSecondaryContainer = Color(0xFFE9DDFF),
    background = UTrackerDeep,
    onBackground = Color(0xFFF1EAF8),
    surface = UTrackerSurface,
    onSurface = Color(0xFFF1EAF8),
    surfaceVariant = UTrackerSurfaceHigh,
    onSurfaceVariant = Color(0xFFD0C4DC),
    outline = UTrackerOutline,
    outlineVariant = Color(0xFF403650),
)

@Composable
fun UltimateTrackerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = UTrackerDarkColors,
        content = content,
    )
}
