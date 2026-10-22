package com.minijarvis.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val SiriColorScheme = darkColorScheme(
    primary = SiriCyan,
    onPrimary = SiriBackground,
    primaryContainer = SiriIndigo,
    onPrimaryContainer = SiriTextPrimary,
    secondary = SiriBlue,
    onSecondary = SiriBackground,
    secondaryContainer = SiriSurfaceGlass,
    onSecondaryContainer = SiriTextPrimary,
    tertiary = SiriMagenta,
    onTertiary = SiriBackground,
    background = SiriBackground,
    onBackground = SiriTextPrimary,
    surface = SiriSurfaceGlass,
    onSurface = SiriTextPrimary,
    surfaceVariant = SiriSurfaceElevated,
    onSurfaceVariant = SiriTextSecondary,
    outline = SiriBorderGlass,
    error = SiriRed,
    onError = SiriBackground
)

@Composable
fun MiniJarvisTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SiriColorScheme,
        typography = Typography,
        content = content
    )
}
