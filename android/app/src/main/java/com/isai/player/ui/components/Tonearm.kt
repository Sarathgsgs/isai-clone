package com.isai.player.ui.components

import android.graphics.BlurMaskFilter
import android.graphics.Paint
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.isai.player.data.repository.ThemeMode
import com.isai.player.ui.theme.IsaiTheme
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * High-performance Canvas rendering of the Isai Tonearm.
 *
 * All Paths and Brushes are constructed once inside [drawWithCache]. Per-frame animations
 * ([angleDeg] and [lift]) are evaluated exclusively inside the draw phase lambda,
 * guaranteeing zero recomposition per frame.
 */
@Composable
fun Tonearm(
    geometry: TonearmGeometry,
    angleDeg: () -> Float,
    lift: () -> Float,
    colors: TonearmColors,
    modifier: Modifier = Modifier
) {
    Spacer(
        modifier = modifier
            .fillMaxSize()
            .drawWithCache {
                val armLength = geometry.armLength
                val armUnit = armLength / 100f

                // --- Geometry Points in Local AU Space (Pivot = (0,0), Arm Unit = 1) ---
                val tipT = Offset(0f, 100f)
                val tubeEndE = Offset(-3f, 82f)

                // Derived headshell rotation angle so its local axis aligns with vector E -> T:
                val dETx = tipT.x - tubeEndE.x // 3
                val dETy = tipT.y - tubeEndE.y // 18
                val headshellRotationDeg = Math.toDegrees(atan2(dETy.toDouble(), dETx.toDouble())).toFloat() - 90f
                val stylusTipDistance = sqrt(dETx * dETx + dETy * dETy) // ~18.25 AU

                // --- Cached Paths (AU coordinates) ---
                val armTubePath = Path().apply {
                    moveTo(0f, 6f)
                    cubicTo(0f, 38f, 0f, 62f, tubeEndE.x, tubeEndE.y)
                }

                val armTubeHighlightPath = Path().apply {
                    moveTo(-0.5f, 6f)
                    cubicTo(-0.5f, 38f, -0.5f, 62f, tubeEndE.x - 0.5f, tubeEndE.y)
                }

                // Bearing collar segment (trimmed path t = 0.08..0.14)
                val pathMeasure = PathMeasure().apply {
                    setPath(armTubePath, false)
                }
                val totalLength = pathMeasure.length
                val collarPath = Path()
                pathMeasure.getSegment(totalLength * 0.08f, totalLength * 0.14f, collarPath, true)

                // Finger lift path (in local headshell frame)
                val fingerLiftPath = Path().apply {
                    moveTo(5f, 4f)
                    lineTo(12f, 6f)
                    lineTo(13f, 1f)
                }

                // Shadow silhouette path
                val shadowPath = Path().apply {
                    addPath(armTubePath)
                }

                // Counterweight gradient
                val counterweightBrush = Brush.verticalGradient(
                    colors = listOf(colors.metalDark, colors.metalMid, colors.metalDark),
                    startY = -30f,
                    endY = -10f
                )

                // Pivot base radial gradient
                val pivotRadialBrush = Brush.radialGradient(
                    colors = listOf(colors.metalLight, colors.metalMid, colors.metalDark),
                    center = Offset(-1.2f, -1.2f),
                    radius = 10f
                )

                // Native paint for blurred shadow
                val shadowPaint = Paint().apply {
                    isAntiAlias = true
                    color = android.graphics.Color.BLACK
                    style = Paint.Style.STROKE
                }

                onDrawBehind {
                    val currentAngle = angleDeg()
                    val currentLift = lift().coerceIn(0f, 1f)

                    // 1. Draw blurred shadow first in screen space (transformed by lift)
                    val shadowOffsetAu = 2f + 8f * currentLift
                    val blurRadiusAu = (3f + 5f * currentLift).coerceAtLeast(1f)
                    val shadowAlpha = colors.shadowAlphaMax - (colors.shadowAlphaMax - colors.shadowAlphaMin) * currentLift

                    withTransform({
                        translate(geometry.pivot.x, geometry.pivot.y)
                        rotate(currentAngle - 90f, pivot = Offset.Zero)
                        scale(armUnit, armUnit, pivot = Offset.Zero)
                        translate(shadowOffsetAu * 0.4f, shadowOffsetAu)
                    }) {
                        shadowPaint.color = Color.Black.copy(alpha = shadowAlpha).toArgb()
                        shadowPaint.strokeWidth = 3.6f
                        shadowPaint.strokeCap = Paint.Cap.ROUND
                        shadowPaint.maskFilter = BlurMaskFilter(blurRadiusAu, BlurMaskFilter.Blur.NORMAL)

                        drawContext.canvas.nativeCanvas.drawPath(
                            shadowPath.asAndroidPath(),
                            shadowPaint
                        )
                    }

                    // 2. Draw the Tonearm assembly in local AU space
                    withTransform({
                        translate(geometry.pivot.x, geometry.pivot.y)
                        rotate(currentAngle - 90f, pivot = Offset.Zero)
                        scale(armUnit, armUnit, pivot = Offset.Zero)
                    }) {
                        // Part 2: Counterweight rod
                        drawLine(
                            color = colors.metalMid,
                            start = Offset(0f, 0f),
                            end = Offset(0f, -10f),
                            strokeWidth = 2f,
                            cap = StrokeCap.Round
                        )

                        // Part 3: Counterweight
                        drawRoundRect(
                            brush = counterweightBrush,
                            topLeft = Offset(-6f, -30f),
                            size = Size(12f, 20f),
                            cornerRadius = CornerRadius(3f, 3f)
                        )
                        // Counterweight groove lines
                        val grooveAlpha = 0.25f
                        val grooveColor = colors.metalDark.copy(alpha = grooveAlpha)
                        drawLine(grooveColor, Offset(-6f, -26f), Offset(6f, -26f), strokeWidth = 0.8f)
                        drawLine(grooveColor, Offset(-6f, -22f), Offset(6f, -22f), strokeWidth = 0.8f)
                        drawLine(grooveColor, Offset(-6f, -18f), Offset(6f, -18f), strokeWidth = 0.8f)

                        // Part 4: Pivot base
                        drawCircle(
                            brush = pivotRadialBrush,
                            radius = 10f,
                            center = Offset.Zero
                        )
                        // Inner ring
                        drawCircle(
                            color = colors.metalDark,
                            radius = 6.5f,
                            center = Offset.Zero,
                            style = Stroke(width = 0.8f)
                        )
                        // Cap circle
                        drawCircle(
                            color = colors.metalMid,
                            radius = 3.5f,
                            center = Offset.Zero
                        )
                        // Highlight dot
                        drawCircle(
                            color = colors.metalLight,
                            radius = 1f,
                            center = Offset(-1.2f, -1.2f)
                        )

                        // Part 5: Arm tube (stacked 3 round-cap strokes for chrome look)
                        drawPath(armTubePath, colors.metalDark, style = Stroke(width = 3.4f, cap = StrokeCap.Round))
                        drawPath(armTubePath, colors.metalMid, style = Stroke(width = 2.2f, cap = StrokeCap.Round))
                        drawPath(armTubeHighlightPath, colors.metalLight, style = Stroke(width = 0.8f, cap = StrokeCap.Round))

                        // Part 6: Bearing collar
                        drawPath(collarPath, colors.metalDark, style = Stroke(width = 4.6f, cap = StrokeCap.Round))
                        drawPath(collarPath, colors.metalLight, style = Stroke(width = 1.0f, cap = StrokeCap.Round))

                        // Part 7: Tube end cap
                        drawCircle(
                            color = colors.metalMid,
                            radius = 2.4f,
                            center = tubeEndE
                        )

                        // Parts 8-11: Headshell assembly (rotated locally at E)
                        withTransform({
                            translate(tubeEndE.x, tubeEndE.y)
                            rotate(headshellRotationDeg, pivot = Offset.Zero)
                        }) {
                            // Part 8: Headshell body (10 x 13, x in -5..5, y in 0..13, r=2)
                            drawRoundRect(
                                color = colors.headshell,
                                topLeft = Offset(-5f, 0f),
                                size = Size(10f, 13f),
                                cornerRadius = CornerRadius(2f, 2f)
                            )
                            // 1 AU theme accent stripe along left edge
                            drawRoundRect(
                                color = colors.accent,
                                topLeft = Offset(-5f, 0f),
                                size = Size(1.2f, 13f),
                                cornerRadius = CornerRadius(1f, 1f)
                            )

                            // Part 9: Finger lift
                            drawPath(
                                path = fingerLiftPath,
                                color = colors.metalMid,
                                style = Stroke(width = 1.1f, cap = StrokeCap.Round)
                            )

                            // Part 10: Cartridge (7 x 6 at y in 11..17, r=1.2)
                            drawRoundRect(
                                color = colors.cartridge,
                                topLeft = Offset(-3.5f, 11f),
                                size = Size(7f, 6f),
                                cornerRadius = CornerRadius(1.2f, 1.2f)
                            )
                            // 0.6 AU accent line on front edge
                            drawLine(
                                color = colors.accent,
                                start = Offset(-3.5f, 17f),
                                end = Offset(3.5f, 17f),
                                strokeWidth = 0.6f
                            )

                            // Part 11: Stylus line ending exactly at T
                            drawLine(
                                color = colors.metalLight,
                                start = Offset(0f, 17f),
                                end = Offset(0f, stylusTipDistance),
                                strokeWidth = 0.6f,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }
            }
    )
}

@Preview(name = "Tonearm - Dark Chrome Preview", widthDp = 300, heightDp = 300)
@Composable
private fun TonearmDarkPreview() {
    IsaiTheme(themeMode = ThemeMode.DARK) {
        val geometry = TonearmGeometry.forDisc(Offset(150f, 150f), 100f)
        Tonearm(
            geometry = geometry,
            angleDeg = { geometry.alphaOuter },
            lift = { 0f },
            colors = TonearmColors.dark()
        )
    }
}

@Preview(name = "Tonearm - Light Chrome Preview", widthDp = 300, heightDp = 300)
@Composable
private fun TonearmLightPreview() {
    IsaiTheme(themeMode = ThemeMode.LIGHT) {
        val geometry = TonearmGeometry.forDisc(Offset(150f, 150f), 100f)
        Tonearm(
            geometry = geometry,
            angleDeg = { geometry.alphaInner },
            lift = { 0.5f },
            colors = TonearmColors.light()
        )
    }
}

