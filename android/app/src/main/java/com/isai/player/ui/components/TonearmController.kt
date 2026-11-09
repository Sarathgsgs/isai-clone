package com.isai.player.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.isai.player.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * State holder managing tonearm physics, needle drop bounce, parking, and gesture tracking.
 */
@Stable
class TonearmState(
    val geometry: TonearmGeometry,
    private val scope: CoroutineScope
) {
    /** 0f = resting on record groove, 1f = lifted and parked at rest. */
    val lift = Animatable(1f)

    /** Non-null during manual user drag / scrubbing. */
    var dragAngle by mutableStateOf<Float?>(null)
        private set

    /** Slight touch scale-up (1.0 to 1.03) while touched. */
    val touchScale = Animatable(1f)

    /** Oscillation offset for deny shake animation when no track is loaded. */
    val shakeOffset = Animatable(0f)

    val isScrubbing: Boolean get() = dragAngle != null

    private var animationJob: Job? = null

    /**
     * Calculates the real-time display angle of the tonearm in degrees.
     * Evaluated inside draw or graphicsLayer lambdas to eliminate recompositions.
     */
    fun displayAngle(progress: Float): Float {
        val targetTrackAngle = geometry.trackAngle(progress)
        val baseAngle = dragAngle ?: (targetTrackAngle + (geometry.alphaRest - targetTrackAngle) * lift.value)
        return baseAngle + shakeOffset.value
    }

    /**
     * Animates tonearm drop with realistic needle bounce onto the groove corresponding to current progress.
     */
    fun animateLower() {
        animationJob?.cancel()
        animationJob = scope.launch {
            lift.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 0.55f, // Slight overshoot / needle bounce
                    stiffness = 200f
                )
            )
        }
    }

    /**
     * Smoothly lifts the tonearm off the record and parks it at rest angle.
     */
    fun animatePark() {
        animationJob?.cancel()
        animationJob = scope.launch {
            lift.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
            )
        }
    }

    /**
     * Triggers a subtle denial shake (±3 degrees, 3 oscillations over 300 ms) when no track is loaded.
     */
    fun triggerDenyShake() {
        scope.launch {
            shakeOffset.snapTo(0f)
            val oscillationSpec = tween<Float>(50, easing = LinearEasing)
            shakeOffset.animateTo(3f, oscillationSpec)
            shakeOffset.animateTo(-3f, oscillationSpec)
            shakeOffset.animateTo(2f, oscillationSpec)
            shakeOffset.animateTo(-2f, oscillationSpec)
            shakeOffset.animateTo(0f, oscillationSpec)
        }
    }

    fun setDragging(angle: Float?) {
        dragAngle = angle
    }

    fun animateSettleToTrack(targetAngle: Float) {
        val start = dragAngle ?: return
        animationJob?.cancel()
        animationJob = scope.launch {
            val anim = Animatable(start)
            anim.animateTo(targetAngle, tween(150, easing = FastOutSlowInEasing)) {
                dragAngle = value
            }
            dragAngle = null
        }
    }
}

@Composable
fun rememberTonearmState(
    geometry: TonearmGeometry,
    scope: CoroutineScope = rememberCoroutineScope()
): TonearmState {
    return remember(geometry) {
        TonearmState(geometry, scope)
    }
}

/**
 * Interactive tonearm container applying gesture detection on a >= 56dp hit area around headshell,
 * haptic ticks, and accessibility semantics.
 */
@Composable
fun TonearmInteractiveContainer(
    state: TonearmState,
    progress: () -> Float,
    isPlaying: Boolean,
    hasTrack: Boolean,
    onScrubStart: () -> Unit,
    onScrubPreview: (Float) -> Unit,
    onScrubEnd: (commit: Boolean, progress: Float) -> Unit,
    onLiftedStop: () -> Unit,
    onTapToggle: () -> Unit,
    colors: TonearmColors,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val hitRadiusPx = with(density) { 32.dp.toPx() } // >= 56dp diameter target
    val scope = rememberCoroutineScope()

    var lastProgressStep by remember { mutableIntStateOf(-1) }
    var wasOnRecord by remember { mutableStateOf(false) }

    val liftActionLabel = stringResource(id = R.string.semantics_action_lift)
    val dropActionLabel = stringResource(id = R.string.semantics_action_drop)
    val fwdActionLabel = stringResource(id = R.string.semantics_action_forward_10s)
    val rwdActionLabel = stringResource(id = R.string.semantics_action_rewind_10s)
    val playingLabel = stringResource(id = R.string.semantics_playing)
    val pausedLabel = stringResource(id = R.string.semantics_paused)
    val currentProg = progress()
    val tonearmDesc = stringResource(id = R.string.semantics_tonearm_description, (currentProg * 100).toInt())

    Box(
        modifier = modifier
            .fillMaxSize()
            .semantics(mergeDescendants = true) {
                role = Role.Slider
                progressBarRangeInfo = ProgressBarRangeInfo(
                    current = currentProg,
                    range = 0f..1f
                )
                contentDescription = tonearmDesc
                stateDescription = if (isPlaying) playingLabel else pausedLabel
                customActions = listOf(
                    CustomAccessibilityAction(liftActionLabel) {
                        onLiftedStop()
                        true
                    },
                    CustomAccessibilityAction(dropActionLabel) {
                        onScrubEnd(true, 0f)
                        true
                    },
                    CustomAccessibilityAction(fwdActionLabel) {
                        val newProgress = (progress() + 0.05f).coerceIn(0f, 1f)
                        onScrubEnd(true, newProgress)
                        true
                    },
                    CustomAccessibilityAction(rwdActionLabel) {
                        val newProgress = (progress() - 0.05f).coerceIn(0f, 1f)
                        onScrubEnd(true, newProgress)
                        true
                    }
                )
            }
            .pointerInput(state.geometry, hasTrack) {
                val pivot = state.geometry.pivot
                val armUnit = state.geometry.armLength / 100f
                val headshellMidAu = Offset(-1.5f, 91f) // Center of headshell in AU space

                awaitEachGesture {
                    val down: PointerInputChange = awaitFirstDown(requireUnconsumed = false)

                    if (!hasTrack) {
                        state.triggerDenyShake()
                        return@awaitEachGesture
                    }

                    // Compute current headshell center in screen space
                    val currentDisplayAngle = state.displayAngle(progress())
                    val rotRad = Math.toRadians((currentDisplayAngle - 90f).toDouble())
                    val cosR = cos(rotRad).toFloat()
                    val sinR = sin(rotRad).toFloat()

                    // Rotate AU point around (0,0), scale by armUnit, translate by pivot
                    val rx = headshellMidAu.x * cosR - headshellMidAu.y * sinR
                    val ry = headshellMidAu.x * sinR + headshellMidAu.y * cosR
                    val headshellCenter = Offset(pivot.x + rx * armUnit, pivot.y + ry * armUnit)

                    val dist = (down.position - headshellCenter).getDistance()
                    if (dist > hitRadiusPx) {
                        // Pointer is outside the 56dp headshell touch target
                        return@awaitEachGesture
                    }

                    down.consume()
                    scope.launch { state.touchScale.animateTo(1.03f, tween(100)) }

                    val velocityTracker = VelocityTracker()
                    velocityTracker.addPosition(down.uptimeMillis, down.position)

                    val startAngle = currentDisplayAngle
                    state.setDragging(startAngle)
                    onScrubStart()

                    val startOnRecord = state.geometry.isOnRecord(startAngle)
                    wasOnRecord = startOnRecord
                    lastProgressStep = if (startOnRecord) {
                        (state.geometry.progressFromAngle(startAngle) * 20).toInt()
                    } else -1

                    var totalMovement = 0f
                    var lastPointerPos = down.position
                    var isDrag = false

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break

                        if (change.pressed != change.previousPressed && !change.pressed) {
                            // Touch released (Up event)
                            change.consume()
                            scope.launch { state.touchScale.animateTo(1.0f, tween(100)) }

                            val velocity = velocityTracker.calculateVelocity()
                            val speedTowardRest = if (state.geometry.alphaRest < state.geometry.alphaOuter) {
                                -velocity.x
                            } else {
                                velocity.x
                            }

                            if (!isDrag && totalMovement < with(density) { 8.dp.toPx() }) {
                                // Tap gesture detected
                                state.setDragging(null)
                                onTapToggle()
                            } else if (speedTowardRest > 1500f) {
                                // Fast fling towards rest -> park & stop
                                state.setDragging(null)
                                onLiftedStop()
                            } else {
                                val finalAngle = state.dragAngle ?: startAngle
                                if (state.geometry.isOnRecord(finalAngle)) {
                                    val finalProgress = state.geometry.progressFromAngle(finalAngle)
                                    val targetTrackAngle = state.geometry.trackAngle(finalProgress)
                                    state.animateSettleToTrack(targetTrackAngle)
                                    onScrubEnd(true, finalProgress)
                                } else {
                                    // Released off the record in the air
                                    state.setDragging(null)
                                    onLiftedStop()
                                }
                            }
                            break
                        }

                        if (change.position != lastPointerPos) {
                            totalMovement += (change.position - lastPointerPos).getDistance()
                            if (totalMovement > with(density) { 8.dp.toPx() }) {
                                isDrag = true
                            }
                            lastPointerPos = change.position
                            velocityTracker.addPosition(change.uptimeMillis, change.position)
                            change.consume()

                            // Pointer to angle
                            val pdx = change.position.x - pivot.x
                            val pdy = change.position.y - pivot.y
                            val rawAngle = Math.toDegrees(atan2(pdy.toDouble(), pdx.toDouble())).toFloat()

                            val minA = minOf(state.geometry.alphaRest, state.geometry.alphaInner)
                            val maxA = maxOf(state.geometry.alphaRest, state.geometry.alphaInner)
                            val clampedAngle = rawAngle.coerceIn(minA, maxA)

                            state.setDragging(clampedAngle)

                            val currentlyOnRecord = state.geometry.isOnRecord(clampedAngle)
                            // Edge crossing tactile feedback
                            if (currentlyOnRecord != wasOnRecord) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                wasOnRecord = currentlyOnRecord
                            }

                            if (currentlyOnRecord) {
                                val previewProg = state.geometry.progressFromAngle(clampedAngle)
                                onScrubPreview(previewProg)

                                val step = (previewProg * 20).toInt() // Every 5%
                                if (step != lastProgressStep) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    lastProgressStep = step
                                }
                            }
                        }
                    }
                }
            }
    ) {
        Tonearm(
            geometry = state.geometry,
            angleDeg = { state.displayAngle(progress()) },
            lift = { state.lift.value },
            colors = colors,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val scale = state.touchScale.value
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(
                        pivotFractionX = state.geometry.pivot.x / size.width.coerceAtLeast(1f),
                        pivotFractionY = state.geometry.pivot.y / size.height.coerceAtLeast(1f)
                    )
                }
        )
    }
}

