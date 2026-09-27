package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = ArenaPrimary,
    onPrimary = Color.White,
    primaryContainer = ArenaPrimaryVariant,
    onPrimaryContainer = Color.White,
    secondary = ArenaSecondary,
    onSecondary = ArenaMidnight,
    tertiary = ArenaTertiary,
    background = ArenaMidnight,
    onBackground = TextPrimaryDark,
    surface = ArenaDarkSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = ArenaDarkSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    outline = ArenaDarkBorder
)

private val LightColorScheme = lightColorScheme(
    primary = ArenaPrimary,
    onPrimary = Color.White,
    primaryContainer = ArenaLightSurfaceVariant,
    onPrimaryContainer = ArenaPrimary,
    secondary = ArenaSecondary,
    onSecondary = Color.White,
    tertiary = ArenaTertiary,
    background = ArenaLightBg,
    onBackground = TextPrimaryLight,
    surface = ArenaLightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = ArenaLightSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    outline = ArenaLightBorder
)

enum class ThemeMode {
    SYSTEM, DARK, LIGHT
}

@Composable
fun MyApplicationTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemDark
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDark -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MyApplicationTheme(
        themeMode = if (darkTheme) ThemeMode.DARK else ThemeMode.LIGHT,
        dynamicColor = dynamicColor,
        content = content
    )
}
