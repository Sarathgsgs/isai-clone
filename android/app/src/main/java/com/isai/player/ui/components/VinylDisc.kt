package com.isai.player.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.isai.player.R
import com.isai.player.data.repository.ThemeMode
import com.isai.player.ui.theme.IsaiTheme
import kotlin.math.abs

/**
 * Photorealistic Vinyl Record composable featuring full-bleed album art,
 * 48 concentric grooves, metallic spindle, and a static light sheen reflection.
 */
@Composable
fun VinylDisc(
    state: VinylDiscState,
    art: Any?,
    fallbackSeed: String,
    colors: VinylColors,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val strokeWidthPx = with(density) { 0.6.dp.toPx() }
    val rimEdgeStrokePx = with(density) { 0.5.dp.toPx() }
    val spindleRingStrokePx = with(density) { 0.8.dp.toPx() }
    val discContentDesc = stringResource(id = R.string.semantics_vinyl_disc_idle)

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .semantics { contentDescription = discContentDesc }
            // Layer 1: Static Ambient Glow / Shadow beneath the record (NOT rotating)
            .drawBehind {
                val r = size.minDimension / 2f
                val center = Offset(size.width / 2f, size.height / 2f)

                if (colors.isDark) {
                    // Dark theme: soft radial accent glow extending ~0.12R beyond edge
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                colors.glowOrShadow,
                                colors.glowOrShadow.copy(alpha = 0.08f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = r * 1.12f
                        ),
                        radius = r * 1.12f,
                        center = center
                    )
                } else {
                    // Light theme: soft tinted shadow offset down by ~0.06R
                    drawCircle(
                        color = colors.glowOrShadow,
                        radius = r * 0.98f,
                        center = Offset(center.x, center.y + r * 0.06f)
                    )
                }
            }
    ) {
        // Layer 2: Rotating Record Layer (Album Art + Grooves + Spindle)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    // Only graphicsLayer property changes per frame -> zero recompositions
                    rotationZ = state.angle
                }
                .clip(CircleShape)
        ) {
            // Full-bleed album art
            if (art != null) {
                SubcomposeAsyncImage(
                    model = art,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    error = {
                        VinylFallbackArt(seed = fallbackSeed)
                    },
                    loading = {
                        VinylFallbackArt(seed = fallbackSeed)
                    }
                )
            } else {
                VinylFallbackArt(seed = fallbackSeed)
            }

            // Grooves and Spindle overlay (pre-cached per size)
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithCache {
                        val r = size.minDimension / 2f
                        val center = Offset(size.width / 2f, size.height / 2f)

                        // 3 Track gap bands around 0.38R, 0.58R, 0.78R
                        val gapBands = listOf(0.38f * r, 0.58f * r, 0.78f * r)
                        val gapThreshold = 0.025f * r

                        onDrawBehind {
                            // 12% black scrim for realistic vinyl contrast
                            drawRect(Color.Black.copy(alpha = 0.12f))

                            // 48 concentric groove rings from 0.96R down to 0.20R
                            val ringCount = 48
                            val step = (0.96f - 0.20f) * r / ringCount

                            for (i in 0 until ringCount) {
                                val currentR = 0.96f * r - i * step

                                // Skip if inside a track gap band
                                val inGap = gapBands.any { abs(currentR - it) < gapThreshold }
                                if (!inGap) {
                                    val isLightRing = (i % 2 == 0)
                                    val pseudoRandomAlpha = 0.03f + 0.07f * (((i * 37 + 17) % 100) / 100f)
                                    val ringColor = if (isLightRing) {
                                        colors.grooveLight.copy(alpha = pseudoRandomAlpha)
                                    } else {
                                        colors.grooveDark.copy(alpha = pseudoRandomAlpha)
                                    }

                                    drawCircle(
                                        color = ringColor,
                                        radius = currentR,
                                        center = center,
                                        style = Stroke(width = strokeWidthPx)
                                    )
                                }
                            }

                            // 3 track gap bands (3x wider, black alpha 0.10)
                            gapBands.forEach { gapR ->
                                drawCircle(
                                    color = Color.Black.copy(alpha = 0.10f),
                                    radius = gapR,
                                    center = center,
                                    style = Stroke(width = strokeWidthPx * 3f)
                                )
                            }

                            // Outer rim ring (0.965R .. R) with 0.5dp edge highlight
                            drawCircle(
                                color = colors.rimDark,
                                radius = r * 0.9825f,
                                center = center,
                                style = Stroke(width = r * 0.035f)
                            )
                            drawCircle(
                                color = colors.rimHighlight,
                                radius = r - rimEdgeStrokePx / 2f,
                                center = center,
                                style = Stroke(width = rimEdgeStrokePx)
                            )

                            // Inner label ring (0.12R .. 0.20R)
                            drawCircle(
                                color = Color.Black.copy(alpha = 0.30f),
                                radius = r * 0.16f,
                                center = center,
                                style = Stroke(width = r * 0.08f)
                            )

                            // Spindle hole (r = 0.03R dark circle with metallic ring)
                            val spindleR = r * 0.03f
                            drawCircle(
                                color = colors.spindleDark,
                                radius = spindleR,
                                center = center
                            )
                            drawCircle(
                                color = colors.spindleRing,
                                radius = spindleR,
                                center = center,
                                style = Stroke(width = spindleRingStrokePx)
                            )
                        }
                    }
            ) {}
        }

        // Layer 3: Static Light Sheen Layer (Does NOT rotate)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .drawWithCache {
                    val r = size.minDimension / 2f
                    val center = Offset(size.width / 2f, size.height / 2f)

                    // Two fixed wedge highlights: 20°..50° and 200°..230°
                    val sheenBrush = Brush.sweepGradient(
                        0.00f to Color.Transparent,
                        0.05f to colors.sheenLight,
                        0.14f to Color.Transparent,
                        0.50f to Color.Transparent,
                        0.55f to colors.sheenLight,
                        0.64f to Color.Transparent,
                        1.00f to Color.Transparent,
                        center = center
                    )

                    // Faint radial vignette
                    val vignetteBrush = Brush.radialGradient(
                        0.0f to Color.Transparent,
                        0.8f to Color.Transparent,
                        1.0f to colors.vignetteEdge,
                        center = center,
                        radius = r
                    )

                    onDrawBehind {
                        drawRect(brush = sheenBrush)
                        drawRect(brush = vignetteBrush)
                    }
                }
        )
    }
}

@Preview(name = "VinylDisc - Dark Theme", widthDp = 300, heightDp = 300)
@Composable
private fun VinylDiscDarkPreview() {
    IsaiTheme(themeMode = ThemeMode.DARK) {
        val state = rememberVinylDiscState()
        VinylDisc(
            state = state,
            art = null,
            fallbackSeed = "Thalapathi Ilaiyaraaja",
            colors = VinylColors.dark()
        )
    }
}

@Preview(name = "VinylDisc - Light Theme", widthDp = 300, heightDp = 300)
@Composable
private fun VinylDiscLightPreview() {
    IsaiTheme(themeMode = ThemeMode.LIGHT) {
        val state = rememberVinylDiscState()
        VinylDisc(
            state = state,
            art = null,
            fallbackSeed = "Roja Rahman",
            colors = VinylColors.light()
        )
    }
}
