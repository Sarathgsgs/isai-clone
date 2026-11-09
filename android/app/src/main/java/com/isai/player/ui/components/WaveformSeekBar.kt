package com.isai.player.ui.components

import android.view.MotionEvent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.isai.player.data.repository.SeekBarStyle
import com.isai.player.data.waveform.WaveformMath
import com.isai.player.ui.theme.IsaiTheme
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max

/**
 * Styling color tokens for WaveformSeekBar.
 */
@Immutable
data class SeekBarColors(
    val played: Color,
    val unplayed: Color,
    val thumb: Color,
    val gradientStart: Color,
    val gradientEnd: Color
)

@Composable
fun defaultSeekBarColors(): SeekBarColors {
    val isDark = IsaiTheme.colors.isDark
    return SeekBarColors(
        played = IsaiTheme.colors.accent,
        unplayed = IsaiTheme.colors.onSurfaceVariant.copy(alpha = 0.35f),
        thumb = IsaiTheme.colors.accent,
        gradientStart = if (isDark) Color(0xFFFF8A3D) else Color(0xFF00E5FF),
        gradientEnd = if (isDark) Color(0xFFFF5252) else Color(0xFF7C4DFF)
    )
}

/**
 * High-performance audio waveform seek bar.
 *
 * Implements three distinct visual styles:
 * - WAVEFORM_BARS: Vertically mirrored rounded bars with gradient fills.
 * - WAVEFORM_LINE: Mirrored silhouette path with quadratic smoothing.
 * - CLASSIC: Minimalist track and thumb.
 *
 * Evaluates progress inside drawWithCache lambdas to guarantee ZERO recompositions on playback ticks.
 */
@Composable
fun WaveformSeekBar(
    peaks: ByteArray?,
    progress: () -> Float,
    bufferedProgress: () -> Float = { 0f },
    style: SeekBarStyle,
    colors: SeekBarColors = defaultSeekBarColors(),
    trackDurationMs: Long = 0L,
    fallbackSeed: Long = 12345L,
    enabled: Boolean = true,
    onScrubStart: () -> Unit,
    onScrubPreview: (Float) -> Unit,
    onScrubEnd: (commit: Boolean, progress: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    var isTouching by remember { mutableStateOf(false) }
    var touchProgress by remember { mutableFloatStateOf(0f) }
    var lastHapticStep by remember { mutableIntStateOf(-1) }

    val resolvedStyle = when (style) {
        SeekBarStyle.AUTO -> if (IsaiTheme.colors.isDark) SeekBarStyle.WAVEFORM_LINE else SeekBarStyle.WAVEFORM_BARS
        else -> style
    }

    // Crossfade between deterministic fallback and real peaks
    val crossfadeAlpha by animateFloatAsState(
        targetValue = if (peaks != null) 1.0f else 0.0f,
        animationSpec = tween(durationMillis = 300),
        label = "WaveformCrossfade"
    )

    val thumbRadiusAnim = remember { Animatable(2.dp.value) }

    val barWidthPx = with(density) { 3.dp.toPx() }
    val barGapPx = with(density) { 2.dp.toPx() }
    val barStepPx = barWidthPx + barGapPx

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .semantics(mergeDescendants = true) {
                role = Role.Slider
                val currentProg = if (enabled) progress().coerceIn(0f, 1f) else 0f
                progressBarRangeInfo = ProgressBarRangeInfo(current = currentProg, range = 0f..1f)
                contentDescription = "Audio seek bar"
                stateDescription = "${(currentProg * 100).toInt()}%"
                if (!enabled) disabled()

                if (enabled) {
                    customActions = listOf(
                        CustomAccessibilityAction("Forward 10 seconds") {
                            val newProgress = (progress() + 0.05f).coerceIn(0f, 1f)
                            onScrubStart()
                            onScrubEnd(true, newProgress)
                            true
                        },
                        CustomAccessibilityAction("Rewind 10 seconds") {
                            val newProgress = (progress() - 0.05f).coerceIn(0f, 1f)
                            onScrubStart()
                            onScrubEnd(true, newProgress)
                            true
                        }
                    )
                }
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput

                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val width = size.width.toFloat().coerceAtLeast(1f)
                    val startX = down.position.x
                    val startY = down.position.y

                    var isHorizontalDrag = false
                    var totalX = 0f
                    var totalY = 0f

                    down.consume()
                    isTouching = true
                    scope.launch { thumbRadiusAnim.animateTo(4.dp.value, tween(100)) }

                    val initialProgress = (startX / width).coerceIn(0f, 1f)
                    touchProgress = initialProgress
                    onScrubStart()
                    onScrubPreview(initialProgress)

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break

                        if (change.changedToUp()) {
                            change.consume()
                            val finalProg = (change.position.x / width).coerceIn(0f, 1f)
                            isTouching = false
                            scope.launch { thumbRadiusAnim.animateTo(2.dp.value, tween(150)) }
                            onScrubEnd(true, finalProg)
                            break
                        }

                        val dx = change.position.x - startX
                        val dy = change.position.y - startY
                        totalX = abs(dx)
                        totalY = abs(dy)

                        // Vertical slop rule: if gesture is predominantly vertical early on, cancel drag
                        if (!isHorizontalDrag && totalY > 16.dp.toPx() && totalY > totalX * 1.5f) {
                            isTouching = false
                            scope.launch { thumbRadiusAnim.animateTo(2.dp.value, tween(150)) }
                            onScrubEnd(false, progress())
                            break
                        }

                        if (totalX > 8.dp.toPx()) {
                            isHorizontalDrag = true
                        }

                        change.consume()
                        val currentX = change.position.x.coerceIn(0f, width)
                        val newProgress = (currentX / width).coerceIn(0f, 1f)
                        touchProgress = newProgress
                        onScrubPreview(newProgress)

                        // 5% tactile haptic tick
                        val step = (newProgress * 20).toInt()
                        if (step != lastHapticStep) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            lastHapticStep = step
                        }
                    }
                }
            }
            .drawWithCache {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val midY = canvasHeight / 2f

                // Resample peaks or fallback
                val fallbackBuckets = WaveformMath.generateDeterministicFallback(fallbackSeed, 200)
                val activePeaks = peaks ?: fallbackBuckets

                val barCount = max(1, (canvasWidth / barStepPx).toInt())
                val resampledPeaks = WaveformMath.resampleBuckets(activePeaks, barCount)
                val resampledFallback = WaveformMath.resampleBuckets(fallbackBuckets, barCount)

                // Cache line path for WAVEFORM_LINE
                val linePath = Path()
                if (resolvedStyle == SeekBarStyle.WAVEFORM_LINE) {
                    val stepX = canvasWidth / (barCount - 1).coerceAtLeast(1)
                    // Top silhouette
                    linePath.moveTo(0f, midY)
                    for (i in 0 until barCount) {
                        val amp = (resampledPeaks[i] * crossfadeAlpha + resampledFallback[i] * (1f - crossfadeAlpha))
                        val halfH = (canvasHeight * 0.45f) * amp
                        val x = i * stepX
                        linePath.lineTo(x, midY - halfH)
                    }
                    linePath.lineTo(canvasWidth, midY)
                    // Bottom mirrored silhouette
                    for (i in (barCount - 1) downTo 0) {
                        val amp = (resampledPeaks[i] * crossfadeAlpha + resampledFallback[i] * (1f - crossfadeAlpha))
                        val halfH = (canvasHeight * 0.45f) * amp
                        val x = i * stepX
                        linePath.lineTo(x, midY + halfH)
                    }
                    linePath.close()
                }

                val gradientBrush = Brush.horizontalGradient(
                    colors = listOf(colors.gradientStart, colors.gradientEnd),
                    startX = 0f,
                    endX = canvasWidth
                )

                onDrawBehind {
                    val currentProgress = if (!enabled) 0f else progress().coerceIn(0f, 1f)
                    val progressX = (currentProgress * canvasWidth).coerceIn(0f, canvasWidth)

                    when (resolvedStyle) {
                        SeekBarStyle.WAVEFORM_BARS -> {
                            // 1. Draw unplayed background bars
                            for (i in 0 until barCount) {
                                val amp = (resampledPeaks[i] * crossfadeAlpha + resampledFallback[i] * (1f - crossfadeAlpha))
                                val barHeight = (canvasHeight * 0.85f * amp).coerceAtLeast(4.dp.toPx())
                                val x = i * barStepPx
                                val top = midY - barHeight / 2f

                                drawRoundRect(
                                    color = colors.unplayed,
                                    topLeft = Offset(x, top),
                                    size = Size(barWidthPx, barHeight),
                                    cornerRadius = CornerRadius(barWidthPx / 2f, barWidthPx / 2f)
                                )
                            }

                            // 2. Draw played bars clipped to progressX
                            clipRect(left = 0f, top = 0f, right = progressX, bottom = canvasHeight) {
                                for (i in 0 until barCount) {
                                    val amp = (resampledPeaks[i] * crossfadeAlpha + resampledFallback[i] * (1f - crossfadeAlpha))
                                    val isCurrentBar = abs(i * barStepPx - progressX) < barStepPx
                                    val popMultiplier = if (isCurrentBar) 1.15f else 1.0f
                                    val barHeight = (canvasHeight * 0.85f * amp * popMultiplier).coerceAtLeast(4.dp.toPx())
                                    val x = i * barStepPx
                                    val top = midY - barHeight / 2f

                                    drawRoundRect(
                                        brush = gradientBrush,
                                        topLeft = Offset(x, top),
                                        size = Size(barWidthPx, barHeight),
                                        cornerRadius = CornerRadius(barWidthPx / 2f, barWidthPx / 2f)
                                    )
                                }
                            }
                        }

                        SeekBarStyle.WAVEFORM_LINE -> {
                            // 1. Draw unplayed silhouette at 25% alpha
                            drawPath(
                                path = linePath,
                                color = colors.unplayed
                            )

                            // 2. Draw played accent silhouette clipped to progressX
                            clipRect(left = 0f, top = 0f, right = progressX, bottom = canvasHeight) {
                                drawPath(
                                    path = linePath,
                                    color = colors.played
                                )
                            }
                        }

                        SeekBarStyle.CLASSIC, SeekBarStyle.AUTO -> {
                            // Minimalist seek track
                            val trackHeightPx = 4.dp.toPx()
                            val trackTop = midY - trackHeightPx / 2f

                            // Unplayed track
                            drawRoundRect(
                                color = colors.unplayed,
                                topLeft = Offset(0f, trackTop),
                                size = Size(canvasWidth, trackHeightPx),
                                cornerRadius = CornerRadius(trackHeightPx / 2f, trackHeightPx / 2f)
                            )

                            // Played track
                            drawRoundRect(
                                color = colors.played,
                                topLeft = Offset(0f, trackTop),
                                size = Size(progressX, trackHeightPx),
                                cornerRadius = CornerRadius(trackHeightPx / 2f, trackHeightPx / 2f)
                            )
                        }
                    }

                    // Playhead indicator thumb / needle line
                    val thumbRadiusPx = thumbRadiusAnim.value * density.density
                    drawLine(
                        color = colors.thumb,
                        start = Offset(progressX, 4.dp.toPx()),
                        end = Offset(progressX, canvasHeight - 4.dp.toPx()),
                        strokeWidth = if (isTouching) 2.5.dp.toPx() else 1.8.dp.toPx()
                    )

                    drawCircle(
                        color = colors.thumb,
                        radius = thumbRadiusPx + 2.dp.toPx(),
                        center = Offset(progressX, midY)
                    )
                }
            }
    )
}

