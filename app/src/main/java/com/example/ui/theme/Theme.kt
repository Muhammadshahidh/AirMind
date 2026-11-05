package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = CyberCyan,
    onPrimary = BackgroundDark,
    primaryContainer = DeepNeonBlue,
    onPrimaryContainer = CyberCyanLight,
    secondary = ElectricBlue,
    onSecondary = TextWhite,
    secondaryContainer = CardBackground,
    onSecondaryContainer = CyberCyan,
    tertiary = NeonPurple,
    onTertiary = TextWhite,
    background = BackgroundDark,
    onBackground = TextWhite,
    surface = SurfaceDark,
    onSurface = TextWhite,
    surfaceVariant = CardBackground,
    onSurfaceVariant = TextGray,
    outline = CardBorder,
    outlineVariant = CardBorderGlow,
    error = NeonRed,
    onError = TextWhite
)

private val LightColorScheme = darkColorScheme(
    // Keep high contrast cyber theme as the default primary aesthetic
    primary = CyberCyan,
    onPrimary = BackgroundDark,
    background = BackgroundDark,
    surface = SurfaceDark,
    onBackground = TextWhite,
    onSurface = TextWhite
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to premium dark aesthetic
    dynamicColor: Boolean = false, // Keep cohesive neon branding
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                @Suppress("DEPRECATION")
                window.statusBarColor = BackgroundDark.toArgb()
                @Suppress("DEPRECATION")
                window.navigationBarColor = BackgroundDark.toArgb()
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = false
                controller.isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
