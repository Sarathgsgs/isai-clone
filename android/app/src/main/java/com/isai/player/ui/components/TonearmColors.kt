package com.isai.player.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.isai.player.ui.theme.CartridgeColor
import com.isai.player.ui.theme.DarkAccentDefault
import com.isai.player.ui.theme.HeadshellColor
import com.isai.player.ui.theme.IsaiTheme
import com.isai.player.ui.theme.LightAccentDefault
import com.isai.player.ui.theme.MetalDark
import com.isai.player.ui.theme.MetalLight
import com.isai.player.ui.theme.MetalMid

@Immutable
data class TonearmColors(
    val metalLight: Color,
    val metalMid: Color,
    val metalDark: Color,
    val headshell: Color,
    val cartridge: Color,
    val accent: Color,
    val shadowAlphaMax: Float,
    val shadowAlphaMin: Float,
    val isDark: Boolean
) {
    companion object {
        fun dark(accent: Color = DarkAccentDefault): TonearmColors = TonearmColors(
            metalLight = MetalLight,
            metalMid = MetalMid,
            metalDark = MetalDark,
            headshell = HeadshellColor,
            cartridge = CartridgeColor,
            accent = accent,
            shadowAlphaMax = 0.25f,
            shadowAlphaMin = 0.12f,
            isDark = true
        )

        fun light(accent: Color = LightAccentDefault): TonearmColors = TonearmColors(
            metalLight = MetalLight,
            metalMid = MetalMid,
            metalDark = MetalDark,
            headshell = HeadshellColor,
            cartridge = CartridgeColor,
            accent = accent,
            shadowAlphaMax = 0.18f,
            shadowAlphaMin = 0.08f,
            isDark = false
        )

        @Composable
        fun current(): TonearmColors {
            val colors = IsaiTheme.colors
            return if (colors.isDark) dark(colors.accent) else light(colors.accent)
        }
    }
}

