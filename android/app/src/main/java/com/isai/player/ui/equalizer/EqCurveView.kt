package com.isai.player.ui.equalizer

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun EqCurveView(
    bandGainsMilliBels: List<Int>,
    minLevelMilliBels: Int,
    maxLevelMilliBels: Int,
    enabled: Boolean,
    onBandGainChanged: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    val view = LocalView.current
    var activeDraggingIndex by remember { mutableIntStateOf(-1) }

    val effectiveMaxRange = maxOf(abs(minLevelMilliBels), abs(maxLevelMilliBels), 1500)
    val curveColor = if (enabled) accentColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    val surfaceColor = MaterialTheme.colorScheme.surface

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .pointerInput(enabled, bandGainsMilliBels) {
                if (!enabled || bandGainsMilliBels.isEmpty()) return@pointerInput

                detectDragGestures(
                    onDragStart = { offset ->
                        val n = bandGainsMilliBels.size
                        val pad = size.width * 0.08f
                        val usableW = size.width - pad * 2
                        val stepX = if (n > 1) usableW / (n - 1) else 0f

                        // Find closest band within touch radius
                        var closestIdx = -1
                        var minDist = Float.MAX_VALUE
                        for (i in 0 until n) {
                            val bx = pad + i * stepX
                            val dist = abs(offset.x - bx)
                            if (dist < minDist && dist < stepX * 0.6f) {
                                minDist = dist
                                closestIdx = i
                            }
                        }
                        activeDraggingIndex = closestIdx
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val idx = activeDraggingIndex
                        if (idx in bandGainsMilliBels.indices) {
                            val centerY = size.height / 2f
                            val maxAmp = (size.height / 2f) * 0.85f

                            // Invert Y delta to change gain
                            val deltaGain = -(dragAmount.y / maxAmp) * effectiveMaxRange
                            val currentGain = bandGainsMilliBels[idx]
                            val newRawGain = (currentGain + deltaGain).roundToInt()
                            // Snap to 50 mB (0.5 dB step)
                            val snappedGain = ((newRawGain + 25) / 50) * 50
                            val clampedGain = snappedGain.coerceIn(minLevelMilliBels, maxLevelMilliBels)

                            // Haptic tick when crossing 0 dB
                            if (currentGain != 0 && clampedGain == 0) {
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            }

                            if (clampedGain != currentGain) {
                                onBandGainChanged(idx, clampedGain)
                            }
                        }
                    },
                    onDragEnd = {
                        activeDraggingIndex = -1
                    },
                    onDragCancel = {
                        activeDraggingIndex = -1
                    }
                )
            }
    ) {
        val w = size.width
        val h = size.height
        val centerY = h / 2f
        val maxAmp = centerY * 0.85f

        // 1. Draw 0 dB baseline (dashed)
        drawLine(
            color = curveColor.copy(alpha = 0.35f),
            start = Offset(0f, centerY),
            end = Offset(w, centerY),
            strokeWidth = 2.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
        )

        val n = bandGainsMilliBels.size
        if (n < 2) return@Canvas

        val pad = w * 0.08f
        val usableW = w - pad * 2
        val stepX = usableW / (n - 1)

        val points = mutableListOf<Offset>()
        for (i in 0 until n) {
            val bx = pad + i * stepX
            val gain = bandGainsMilliBels[i]
            val by = centerY - (gain.toFloat() / effectiveMaxRange) * maxAmp
            points.add(Offset(bx, by))
        }

        // 2. Build Catmull-Rom Spline Path
        val path = Path()
        path.moveTo(points[0].x, points[0].y)

        for (i in 0 until n - 1) {
            val p0 = points[maxOf(0, i - 1)]
            val p1 = points[i]
            val p2 = points[i + 1]
            val p3 = points[minOf(n - 1, i + 2)]

            val c1x = p1.x + (p2.x - p0.x) / 6f
            val c1y = p1.y + (p2.y - p0.y) / 6f
            val c2x = p2.x - (p3.x - p1.x) / 6f
            val c2y = p2.y - (p3.y - p1.y) / 6f

            path.cubicTo(c1x, c1y, c2x, c2y, p2.x, p2.y)
        }

        // 3. Draw glowing gradient fill under curve
        val fillPath = Path().apply {
            addPath(path)
            lineTo(points.last().x, h)
            lineTo(points.first().x, h)
            close()
        }

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    curveColor.copy(alpha = if (enabled) 0.35f else 0.08f),
                    Color.Transparent
                ),
                startY = 0f,
                endY = h
            )
        )

        // 4. Draw main spline curve
        drawPath(
            path = path,
            color = curveColor,
            style = Stroke(
                width = 3.5.dp.toPx(),
                cap = StrokeCap.Round
            )
        )

        // 5. Draw band nodes
        for (i in 0 until n) {
            val pt = points[i]
            val isDragging = i == activeDraggingIndex

            // Outer ring
            drawCircle(
                color = if (isDragging) curveColor else curveColor.copy(alpha = 0.5f),
                radius = if (isDragging) 12.dp.toPx() else 8.dp.toPx(),
                center = pt
            )
            // Inner core
            drawCircle(
                color = if (enabled) surfaceColor else curveColor,
                radius = if (isDragging) 6.dp.toPx() else 4.dp.toPx(),
                center = pt
            )
        }
    }
}

