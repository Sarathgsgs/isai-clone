package com.isai.player.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class VinylDiscStateTest {

    @Test
    fun advanceAngleAtSpeedOneForOneSecondGives200Degrees() {
        val startAngle = 0f
        val speed = 1.0f
        val dtNanos = 1_000_000_000L // 1 second

        // Because dt is clamped to 50ms (0.05s) per step to prevent frame hitches,
        // advancing 20 frames of 50ms gives exactly 1.0 second:
        var angle = startAngle
        val frame50msNanos = 50_000_000L
        for (i in 0 until 20) {
            angle = advanceAngle(angle, speed, frame50msNanos)
        }

        assertEquals(200f, angle, 0.01f)
    }

    @Test
    fun advanceAngleAtHalfSpeedForOneSecondGives100Degrees() {
        val startAngle = 0f
        val speed = 0.5f
        var angle = startAngle
        val frame50msNanos = 50_000_000L
        for (i in 0 until 20) {
            angle = advanceAngle(angle, speed, frame50msNanos)
        }

        assertEquals(100f, angle, 0.01f)
    }

    @Test
    fun angleWrapsAroundAt360Degrees() {
        // 350 degrees + 20 degrees delta -> 370 % 360 = 10 degrees
        // deltaDeg = 200 * speed * dtSeconds => for deltaDeg = 20: dtSeconds = 20 / 200 = 0.1s
        // At 50ms step (10 deg per step), two 50ms steps advance 20 deg:
        var angle = 350f
        val speed = 1.0f
        val frame50msNanos = 50_000_000L

        angle = advanceAngle(angle, speed, frame50msNanos) // 350 + 10 = 360 -> 0
        assertEquals(0f, angle, 0.01f)

        angle = advanceAngle(angle, speed, frame50msNanos) // 0 + 10 = 10
        assertEquals(10f, angle, 0.01f)
    }

    @Test
    fun dtIsClampedAt50MillisecondsToPreventHitchJumping() {
        val startAngle = 0f
        val speed = 1.0f
        val hugeHitchNanos = 500_000_000L // 500ms (half a second hitch)

        // Without clamp: 200 * 1.0 * 0.5 = 100 degrees
        // With clamp (50ms): 200 * 1.0 * 0.05 = 10 degrees
        val angle = advanceAngle(startAngle, speed, hugeHitchNanos)

        assertEquals(10f, angle, 0.01f)
    }

    @Test
    fun speedZeroProducesNoAngleChange() {
        val startAngle = 45f
        val result = advanceAngle(startAngle, speed = 0f, dtNanos = 16_666_666L)
        assertEquals(startAngle, result, 0.001f)
    }
}
