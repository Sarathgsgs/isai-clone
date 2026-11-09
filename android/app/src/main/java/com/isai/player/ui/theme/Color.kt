package com.isai.player.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// --- Core Dark Theme Tokens ---
val DarkBackground = Color(0xFF0B0B0E)
val DarkSurface = Color(0xFF16161B)
val DarkSurfaceVariant = Color(0xFF22222A)
val DarkOnBackground = Color(0xFFF5F5F7)
val DarkOnSurfaceVariant = Color(0xFF9A9AA5)
val DarkAccentDefault = Color(0xFFFF8A3D) // Vibrant Orange

// --- Core Light Theme Tokens ---
val LightBackground = Color(0xFFF3F4FF) // Soft Lavender
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFE8EAFB)
val LightOnBackground = Color(0xFF0F1535) // Deep Navy
val LightOnSurfaceVariant = Color(0xFF525A7A) // WCAG AA compliant on White & Soft Lavender
val LightAccentDefault = Color(0xFF3857DC) // Vivid Royal Blue (WCAG AA >= 4.5:1 on White)

// --- Tonearm & Vinyl Hardware Palette ---
val MetalLight = Color(0xFFF2F4F8)
val MetalMid = Color(0xFFA9AFBA)
val MetalDark = Color(0xFF4A4E57)
val HeadshellColor = Color(0xFF17171B)
val CartridgeColor = Color(0xFF2A2A30)

// --- Ambient Scrims & Glows ---
val DarkAmbientGlow = Color(0x40FF8A3D)
val LightAmbientShadow = Color(0x2E0F1535)
val DarkScrim = Color(0xA6000000)
val LightScrim = Color(0x330F1535)

/**
 * Extended color tokens for custom Isai components, including tonearm metal gradients,
 * disc groove overlays, ambient glows, and dynamic accents.
 */
@Immutable
data class IsaiColors(
    val accent: Color,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val onBackground: Color,
    val onSurfaceVariant: Color,
    val ambientGlow: Color,
    val scrim: Color,
    val metalLight: Color = MetalLight,
    val metalMid: Color = MetalMid,
    val metalDark: Color = MetalDark,
    val headshell: Color = HeadshellColor,
    val cartridge: Color = CartridgeColor,
    val isDark: Boolean
)

val LocalIsaiColors = staticCompositionLocalOf {
    IsaiColors(
        accent = DarkAccentDefault,
        background = DarkBackground,
        surface = DarkSurface,
        surfaceVariant = DarkSurfaceVariant,
        onBackground = DarkOnBackground,
        onSurfaceVariant = DarkOnSurfaceVariant,
        ambientGlow = DarkAmbientGlow,
        scrim = DarkScrim,
        isDark = true
    )
}

