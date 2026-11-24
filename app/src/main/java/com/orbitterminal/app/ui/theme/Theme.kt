package com.orbitterminal.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = OrbitPrimary,
    secondary = OrbitSecondary,
    background = OrbitBackground,
    surface = OrbitSurface,
    surfaceVariant = OrbitSurfaceVariant,
    onPrimary = OrbitOnPrimary,
    onBackground = OrbitOnBackground,
    onSurface = OrbitOnSurface,
    error = OrbitError
)

private val LightColorScheme = lightColorScheme(
    primary = OrbitPrimary,
    secondary = OrbitSecondary,
    background = Color(0xFFF6F7FB),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEBEFF8),
    onPrimary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    error = OrbitError
)

@Composable
fun OrbitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
