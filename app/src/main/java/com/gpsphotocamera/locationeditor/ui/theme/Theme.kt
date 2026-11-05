package com.gpsphotocamera.locationeditor.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = GpsRed,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = GpsRedDark,
    onPrimaryContainer = androidx.compose.ui.graphics.Color.White,
    secondary = AccentGold,
    background = DarkSurface,
    surface = DarkSurfaceElevated,
    surfaceVariant = DarkSurfaceCard,
    outline = DarkBorder,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    error = StatusRed
)

@Composable
fun GPSPhotoCameraTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
