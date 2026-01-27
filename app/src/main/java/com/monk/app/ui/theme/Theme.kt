package com.monk.app.ui.theme

import android.app.Activity
import android.os.Build
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

// Light theme - Warm, refined, understated luxury
private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryLight,
    onPrimaryContainer = OnPrimary,
    
    secondary = Accent,
    onSecondary = Primary,
    secondaryContainer = AccentLight,
    onSecondaryContainer = Primary,
    
    tertiary = Accent,
    onTertiary = Primary,
    tertiaryContainer = AccentLight,
    onTertiaryContainer = Primary,
    
    background = Background,
    onBackground = OnSurface,
    
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    
    error = Error,
    onError = OnPrimary,
    errorContainer = ErrorLight,
    onErrorContainer = Error,
    
    outline = Border,
    outlineVariant = BorderLight,
    
    inverseSurface = Primary,
    inverseOnSurface = OnPrimary,
    inversePrimary = Accent
)

// Dark theme - Deep, sophisticated, intimate
private val DarkColorScheme = darkColorScheme(
    primary = Accent,
    onPrimary = Primary,
    primaryContainer = PrimaryLight,
    onPrimaryContainer = OnPrimary,
    
    secondary = AccentLight,
    onSecondary = Primary,
    secondaryContainer = AccentDark,
    onSecondaryContainer = OnPrimary,
    
    tertiary = AccentLight,
    onTertiary = Primary,
    tertiaryContainer = AccentDark,
    onTertiaryContainer = OnPrimary,
    
    background = Gray900,
    onBackground = Gray100,
    
    surface = Gray800,
    onSurface = Gray100,
    surfaceVariant = Gray700,
    onSurfaceVariant = Gray300,
    
    error = Color(0xFFCF6679),
    onError = Gray900,
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    
    outline = Gray600,
    outlineVariant = Gray700,
    
    inverseSurface = Gray100,
    inverseOnSurface = Gray800,
    inversePrimary = Primary
)

@Composable
fun MonkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Use light background color for status bar
            window.statusBarColor = if (darkTheme) Gray900.toArgb() else Background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
