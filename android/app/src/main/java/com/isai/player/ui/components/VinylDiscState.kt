package com.isai.player.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.flow.collectLatest

/**
 * Pure function advancing rotation angle by 33⅓ RPM (200°/s) * speed multiplier.
 * Clamps [dtNanos] to at most 50ms so a frame hitch never makes the disc jump.
 */
fun advanceAngle(currentAngle: Float, speed: Float, dtNanos: Long): Float {
    if (speed <= 0f || dtNanos <= 0L) return currentAngle
    val dtSeconds = (dtNanos / 1_000_000_000f).coerceAtMost(0.05f) // 50ms clamp
    val deltaDeg = 200f * speed * dtSeconds
    val newAngle = (currentAngle + deltaDeg) % 360f
    return if (newAngle < 0f) newAngle + 360f else newAngle
}

/**
 * State holder managing vinyl rotation physics, speed animation, and frame loop.
 */
@Stable
class VinylDiscState {
    /**
     * Current rotation angle in degrees [0..360).
     * Must be read ONLY inside draw or graphicsLayer lambdas to eliminate recompositions.
     */
    var angle by mutableFloatStateOf(0f)
        private set

    /** Speed multiplier [0..1] where 1.0 represents 33⅓ RPM (200°/s). */
    val speed = Animatable(0f)

    fun resetAngle() {
        angle = 0f
    }

    fun setAngleManual(newAngle: Float) {
        angle = (newAngle % 360f).let { if (it < 0f) it + 360f else it }
    }

    suspend fun spinUp() {
        speed.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 500, easing = FastOutLinearInEasing)
        )
    }

    suspend fun spinDown() {
        speed.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 900, easing = LinearOutSlowInEasing)
        )
    }

    suspend fun snapStop() {
        speed.snapTo(0f)
    }

    fun updateAngleDelta(dtNanos: Long) {
        angle = advanceAngle(angle, speed.value, dtNanos)
    }
}

@Composable
fun rememberVinylDiscState(): VinylDiscState {
    val state = remember { VinylDiscState() }

    // High performance frame loop active only while speed > 0
    LaunchedEffect(state) {
        snapshotFlow { state.speed.value > 0f }.collectLatest { isSpinning ->
            if (isSpinning) {
                var lastFrameNanos = 0L
                while (state.speed.value > 0f) {
                    withFrameNanos { frameNanos ->
                        if (lastFrameNanos > 0L) {
                            val dtNanos = frameNanos - lastFrameNanos
                            state.updateAngleDelta(dtNanos)
                        }
                        lastFrameNanos = frameNanos
                    }
                }
            }
        }
    }

    return state
}

/**
 * Composable helper connecting external playback state to vinyl spin physics.
 */
@Composable
fun VinylDiscState.SpinEffect(spinning: Boolean, reducedMotion: Boolean = false) {
    LaunchedEffect(spinning, reducedMotion) {
        if (reducedMotion) {
            snapStop()
        } else {
            if (spinning) {
                spinUp()
            } else {
                spinDown()
            }
        }
    }
}
