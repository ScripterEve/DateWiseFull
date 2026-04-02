package com.example.datewise.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = DateWiseGreenLight,
    onPrimary = Color.White,
    primaryContainer = DateWiseGreen,
    onPrimaryContainer = Color.White,
    secondary = DateWiseGreenBright,
    onSecondary = Color.Black,
    tertiary = DateWiseGreenAccent,
    background = DarkBackground,
    onBackground = TextOnDark,
    surface = DarkSurface,
    onSurface = TextOnDark,
    surfaceVariant = DarkCard,
    onSurfaceVariant = TextOnDarkSecondary,
    outline = Color(0xFF444444),
    error = ExpiryExpired,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = DateWiseGreen,
    onPrimary = Color.White,
    primaryContainer = DateWiseGreenSurface,
    onPrimaryContainer = DateWiseGreen,
    secondary = DateWiseGreenLight,
    onSecondary = Color.White,
    tertiary = DateWiseGreenAccent,
    background = LightBackground,
    onBackground = TextPrimary,
    surface = LightSurface,
    onSurface = TextPrimary,
    surfaceVariant = LightCard,
    onSurfaceVariant = TextSecondary,
    outline = DividerColor,
    error = ExpiryExpired,
    onError = Color.White
)

@Composable
fun DateWiseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}