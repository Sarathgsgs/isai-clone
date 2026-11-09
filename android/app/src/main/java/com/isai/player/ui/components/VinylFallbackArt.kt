package com.isai.player.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import kotlin.math.abs

/**
 * Deterministic fallback art for tracks without embedded album art.
 * Generates a vibrant diagonal dual-hue gradient derived from [seed] (title + artist)
 * and centers the track/artist initials.
 */
@Composable
fun VinylFallbackArt(
    seed: String,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()

    // Deterministic hue generator based on seed string hash
    val (color1, color2, initials) = remember(seed) {
        val hash = abs(seed.hashCode())
        val hue1 = (hash % 360).toFloat()
        val hue2 = ((hue1 + 40f) % 360f)

        val c1 = Color.hsl(hue1, 0.75f, 0.40f)
        val c2 = Color.hsl(hue2, 0.85f, 0.25f)

        val words = seed.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }
        val letters = when {
            words.size >= 2 -> "${words[0].first()}${words[1].first()}".uppercase()
            words.isNotEmpty() && words[0].length >= 2 -> words[0].take(2).uppercase()
            words.isNotEmpty() -> words[0].take(1).uppercase()
            else -> "IS"
        }
        Triple(c1, c2, letters)
    }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val r = size.minDimension / 2f

            // Diagonal dual-hue gradient
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(color1, color2),
                    start = Offset.Zero,
                    end = Offset(size.width, size.height)
                )
            )

            // Stylized Centered Initials sized to ~0.28R
            val fontSizeSp = (r * 0.28f).sp
            val textLayoutResult = textMeasurer.measure(
                text = initials,
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = fontSizeSp,
                    color = Color.White.copy(alpha = 0.85f)
                )
            )

            val textOffset = Offset(
                x = (size.width - textLayoutResult.size.width) / 2f,
                y = (size.height - textLayoutResult.size.height) / 2f
            )

            drawText(
                textLayoutResult = textLayoutResult,
                topLeft = textOffset
            )
        }
    }
}
