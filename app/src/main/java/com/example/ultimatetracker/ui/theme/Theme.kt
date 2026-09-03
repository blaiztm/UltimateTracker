package com.example.ultimatetracker.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class AppTheme { ORIGINAL, DEEP_DARK, DARK, LIGHT, FOREST, OCEAN, CRIMSON, METAL }
enum class AppIconColor(val alias: String) { ORIGINAL("OriginalIcon"), DEEP_DARK("DeepDarkIcon"), DARK("DarkIcon"), LIGHT("LightIcon"), FOREST("ForestIcon"), OCEAN("OceanIcon"), CRIMSON("CrimsonIcon"), METAL("MetalIcon") }

private fun dark(primary: Long, surface: Long, background: Long, secondary: Long) = darkColorScheme(
    primary = Color(primary), onPrimary = Color(0xFF1D102F), primaryContainer = Color(primary).copy(alpha = .35f), onPrimaryContainer = Color(0xFFF6EEFF),
    secondary = Color(secondary), onSecondary = Color(0xFF201A22), secondaryContainer = Color(secondary).copy(alpha = .28f), onSecondaryContainer = Color(0xFFF8EEF8),
    background = Color(background), onBackground = Color(0xFFECE6EE), surface = Color(surface), onSurface = Color(0xFFECE6EE),
    surfaceVariant = Color(surface + 0x000A0A0AL), onSurfaceVariant = Color(0xFFD0C5D2), outline = Color(0xFF968D99), outlineVariant = Color(0xFF49454E),
)

private val schemes: Map<AppTheme, ColorScheme> = mapOf(
    AppTheme.ORIGINAL to dark(0xFFB99AFF, 0xFF1B1529, 0xFF120E1C, 0xFFD0B8F5),
    AppTheme.DEEP_DARK to dark(0xFFD8C2FF, 0xFF121212, 0xFF000000, 0xFFCBC2D0),
    AppTheme.DARK to dark(0xFFBFC7FF, 0xFF1A1B20, 0xFF121318, 0xFFC4C6D0),
    AppTheme.FOREST to dark(0xFF8FD6A5, 0xFF152019, 0xFF0C1410, 0xFFA9D0B2),
    AppTheme.OCEAN to dark(0xFF80D0FF, 0xFF142027, 0xFF0A141A, 0xFF9CCFE5),
    AppTheme.CRIMSON to dark(0xFFFFB2B9, 0xFF251619, 0xFF1B1012, 0xFFE7B7BA),
    AppTheme.METAL to dark(0xFFD0C6B8, 0xFF20201E, 0xFF151513, 0xFFC9C4BD),
    AppTheme.LIGHT to lightColorScheme(
        primary = Color(0xFF7651AB), onPrimary = Color.White, primaryContainer = Color(0xFFECDDFF), onPrimaryContainer = Color(0xFF2D0A5C),
        secondary = Color(0xFF655A70), onSecondary = Color.White, secondaryContainer = Color(0xFFECDDFF), onSecondaryContainer = Color(0xFF20182B),
        background = Color(0xFFFFF7FF), onBackground = Color(0xFF1D1B20), surface = Color(0xFFFFF7FF), onSurface = Color(0xFF1D1B20),
        surfaceVariant = Color(0xFFE9E0EC), onSurfaceVariant = Color(0xFF4B454F), outline = Color(0xFF7C747F), outlineVariant = Color(0xFFCDC4CF),
    ),
)

@Composable
fun UltimateTrackerTheme(theme: AppTheme, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = schemes.getValue(theme),
        content = content,
    )
}
