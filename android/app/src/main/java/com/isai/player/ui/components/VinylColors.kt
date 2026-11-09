package com.isai.player.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.isai.player.ui.theme.DarkAccentDefault
import com.isai.player.ui.theme.IsaiTheme
import com.isai.player.ui.theme.LightAccentDefault
import com.isai.player.ui.theme.MetalMid

@Immutable
data class VinylColors(
    val glowOrShadow: Color,
    val grooveLight: Color,
    val grooveDark: Color,
    val rimDark: Color,
    val rimHighlight: Color,
    val spindleDark: Color,
    val spindleRing: Color,
    val sheenLight: Color,
    val vignetteEdge: Color,
    val isDark: Boolean
) {
    companion object {
        fun dark(accent: Color = DarkAccentDefault): VinylColors = VinylColors(
            glowOrShadow = accent.copy(alpha = 0.25f),
            grooveLight = Color.White,
            grooveDark = Color.Black,
            rimDark = Color(0x8C0B0B0E), // black alpha 0.55
            rimHighlight = Color(0x2EFFFFFF), // white alpha 0.18
            spindleDark = Color(0xFF0D0D10),
            spindleRing = MetalMid,
            sheenLight = Color.White.copy(alpha = 0.15f),
            vignetteEdge = Color.Black.copy(alpha = 0.20f),
            isDark = true
        )

        fun light(accent: Color = LightAccentDefault): VinylColors = VinylColors(
            glowOrShadow = Color(0x400F1535), // soft dark-tinted shadow alpha 0.25
            grooveLight = Color.White,
            grooveDark = Color(0xCC000000),
            rimDark = Color(0x8C0F1535),
            rimHighlight = Color(0x2EFFFFFF),
            spindleDark = Color(0xFF0D0D10),
            spindleRing = MetalMid,
            sheenLight = Color.White.copy(alpha = 0.15f),
            vignetteEdge = Color.Black.copy(alpha = 0.20f),
            isDark = false
        )

        @Composable
        fun current(): VinylColors {
            val colors = IsaiTheme.colors
            return if (colors.isDark) dark(colors.accent) else light(colors.accent)
        }
    }
}
