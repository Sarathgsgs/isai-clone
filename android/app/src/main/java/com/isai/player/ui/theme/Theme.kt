package com.isai.player.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.isai.player.data.repository.ThemeMode

private fun createDarkColorScheme(accent: Color) = darkColorScheme(
    primary = accent,
    onPrimary = Color.White,
    primaryContainer = accent.copy(alpha = 0.2f),
    onPrimaryContainer = Color.White,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnBackground,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant
)

private fun createLightColorScheme(accent: Color) = lightColorScheme(
    primary = accent,
    onPrimary = Color.White,
    primaryContainer = accent.copy(alpha = 0.15f),
    onPrimaryContainer = LightOnBackground,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnBackground,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant
)

@Composable
fun IsaiTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    accentOverride: Color? = null,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    val context = LocalContext.current
    val accent = accentOverride ?: if (darkTheme) DarkAccentDefault else LightAccentDefault

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> createDarkColorScheme(accent)
        else -> createLightColorScheme(accent)
    }

    val isaiColors = IsaiColors(
        accent = accent,
        background = if (darkTheme) DarkBackground else LightBackground,
        surface = if (darkTheme) DarkSurface else LightSurface,
        surfaceVariant = if (darkTheme) DarkSurfaceVariant else LightSurfaceVariant,
        onBackground = if (darkTheme) DarkOnBackground else LightOnBackground,
        onSurfaceVariant = if (darkTheme) DarkOnSurfaceVariant else LightOnSurfaceVariant,
        ambientGlow = if (darkTheme) DarkAmbientGlow.copy(alpha = 0.3f) else LightAmbientShadow,
        scrim = if (darkTheme) DarkScrim else LightScrim,
        isDark = darkTheme
    )

    CompositionLocalProvider(LocalIsaiColors provides isaiColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}

/**
 * Accessor object for custom Isai theme properties.
 */
object IsaiTheme {
    val colors: IsaiColors
        @Composable
        @ReadOnlyComposable
        get() = LocalIsaiColors.current
}

