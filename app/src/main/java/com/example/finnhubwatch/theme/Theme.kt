package com.example.finnhubwatch.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
    darkColorScheme(
        primary = WatchlistBlueDark,
        onPrimary = Color(0xFF0A305F),
        background = WatchlistDarkBackground,
        onBackground = WatchlistDarkOnBackground,
        surface = WatchlistDarkSurface,
        onSurface = WatchlistDarkOnBackground,
        surfaceVariant = Color(0xFF25272B),
        onSurfaceVariant = WatchlistDarkOnVariant,
        outline = WatchlistDarkOutline,
    )

private val LightColorScheme =
    lightColorScheme(
        primary = WatchlistBlue,
        background = WatchlistBackground,
        onBackground = WatchlistOnBackground,
        surface = WatchlistSurface,
        onSurface = WatchlistOnBackground,
        surfaceVariant = Color(0xFFEEF1F6),
        onSurfaceVariant = WatchlistOnVariant,
        outline = WatchlistOutline,
    )

@Composable
fun FinnhubWatchTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content,
    )
}
