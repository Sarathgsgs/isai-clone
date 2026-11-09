package com.isai.player.ui.accessibility

import androidx.compose.ui.graphics.Color
import com.isai.player.ui.theme.DarkAccentDefault
import com.isai.player.ui.theme.DarkBackground
import com.isai.player.ui.theme.DarkOnBackground
import com.isai.player.ui.theme.DarkOnSurfaceVariant
import com.isai.player.ui.theme.DarkSurface
import com.isai.player.ui.theme.LightAccentDefault
import com.isai.player.ui.theme.LightBackground
import com.isai.player.ui.theme.LightOnBackground
import com.isai.player.ui.theme.LightOnSurfaceVariant
import com.isai.player.ui.theme.LightSurface
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Validates that all Isai theme colors satisfy WCAG 2.1 AA relative luminance contrast ratios:
 * - Normal text & icons: >= 4.5:1
 * - Large UI components & graphical elements: >= 3.0:1
 */
class AccessibilityContrastTest {

    /**
     * Standard W3C relative luminance calculation:
     * L = 0.2126 * R' + 0.7152 * G' + 0.0722 * B'
     */
    private fun relativeLuminance(color: Color): Double {
        fun channel(c: Float): Double {
            val d = c.toDouble()
            return if (d <= 0.04045) {
                d / 12.92
            } else {
                ((d + 0.055) / 1.055).pow(2.4)
            }
        }
        val r = channel(color.red)
        val g = channel(color.green)
        val b = channel(color.blue)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    /**
     * Contrast ratio: (L1 + 0.05) / (L2 + 0.05)
     */
    private fun contrastRatio(fg: Color, bg: Color): Double {
        val l1 = relativeLuminance(fg)
        val l2 = relativeLuminance(bg)
        val lighter = max(l1, l2)
        val darker = min(l1, l2)
        return (lighter + 0.05) / (darker + 0.05)
    }

    @Test
    fun darkThemeContrastMeetsWcagAA() {
        val textOnBg = contrastRatio(DarkOnBackground, DarkBackground)
        val textOnSurface = contrastRatio(DarkOnBackground, DarkSurface)
        val variantOnBg = contrastRatio(DarkOnSurfaceVariant, DarkBackground)
        val variantOnSurface = contrastRatio(DarkOnSurfaceVariant, DarkSurface)
        val accentOnBg = contrastRatio(DarkAccentDefault, DarkBackground)
        val accentOnSurface = contrastRatio(DarkAccentDefault, DarkSurface)

        assertTrue("DarkOnBackground on DarkBackground ($textOnBg) must be >= 4.5", textOnBg >= 4.5)
        assertTrue("DarkOnBackground on DarkSurface ($textOnSurface) must be >= 4.5", textOnSurface >= 4.5)
        assertTrue("DarkOnSurfaceVariant on DarkBackground ($variantOnBg) must be >= 4.5", variantOnBg >= 4.5)
        assertTrue("DarkOnSurfaceVariant on DarkSurface ($variantOnSurface) must be >= 4.5", variantOnSurface >= 4.5)
        assertTrue("DarkAccentDefault on DarkBackground ($accentOnBg) must be >= 3.0", accentOnBg >= 3.0)
        assertTrue("DarkAccentDefault on DarkSurface ($accentOnSurface) must be >= 3.0", accentOnSurface >= 3.0)
    }

    @Test
    fun lightThemeContrastMeetsWcagAA() {
        val textOnBg = contrastRatio(LightOnBackground, LightBackground)
        val textOnSurface = contrastRatio(LightOnBackground, LightSurface)
        val variantOnBg = contrastRatio(LightOnSurfaceVariant, LightBackground)
        val variantOnSurface = contrastRatio(LightOnSurfaceVariant, LightSurface)
        val accentOnBg = contrastRatio(LightAccentDefault, LightBackground)
        val accentOnSurface = contrastRatio(LightAccentDefault, LightSurface)

        assertTrue("LightOnBackground on LightBackground ($textOnBg) must be >= 4.5", textOnBg >= 4.5)
        assertTrue("LightOnBackground on LightSurface ($textOnSurface) must be >= 4.5", textOnSurface >= 4.5)
        assertTrue("LightOnSurfaceVariant on LightBackground ($variantOnBg) must be >= 4.5", variantOnBg >= 4.5)
        assertTrue("LightOnSurfaceVariant on LightSurface ($variantOnSurface) must be >= 4.5", variantOnSurface >= 4.5)
        assertTrue("LightAccentDefault on LightBackground ($accentOnBg) must be >= 4.5", accentOnBg >= 4.5)
        assertTrue("LightAccentDefault on LightSurface ($accentOnSurface) must be >= 4.5", accentOnSurface >= 4.5)
    }
}

