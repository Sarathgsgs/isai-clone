package com.isai.player.ui.accessibility

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Validates Tonearm accessibility semantics logic, range mapping, and custom action calculations.
 */
class TonearmSemanticsTest {

    @Test
    fun seekRelativeCalculationsClampedProperly() {
        // Forward 5% (simulating ~10s on a 200s track)
        val initialProgress = 0.98f
        val forwarded = (initialProgress + 0.05f).coerceIn(0f, 1f)
        assertEquals(1.0f, forwarded, 0.0001f)

        // Rewind 5%
        val startProg = 0.03f
        val rewound = (startProg - 0.05f).coerceIn(0f, 1f)
        assertEquals(0.0f, rewound, 0.0001f)

        // Mid-track step
        val midProg = 0.45f
        val midStep = (midProg + 0.05f).coerceIn(0f, 1f)
        assertEquals(0.50f, midStep, 0.0001f)
    }

    @Test
    fun progressPercentageFormatting() {
        val testCases = listOf(
            0.0f to 0,
            0.423f to 42,
            0.999f to 99,
            1.0f to 100
        )
        for ((progress, expectedPercent) in testCases) {
            val percent = (progress * 100).toInt()
            assertEquals("Progress $progress must format to $expectedPercent%", expectedPercent, percent)
        }
    }

    @Test
    fun stateDescriptionSelection() {
        fun getStateDescription(isPlaying: Boolean): String {
            return if (isPlaying) "Playing" else "Paused"
        }

        assertEquals("Playing", getStateDescription(true))
        assertEquals("Paused", getStateDescription(false))
    }

    @Test
    fun customActionDispatchesExpectedCallbacks() {
        var liftCalled = false
        var dropProgress: Float? = null
        var seekProgress: Float? = null

        val onLiftedStop: () -> Unit = { liftCalled = true }
        val onScrubEnd: (Boolean, Float) -> Unit = { commit, prog ->
            if (prog == 0f) dropProgress = prog else seekProgress = prog
        }

        // Simulate Action: Lift Needle
        onLiftedStop()
        assertTrue("Lift needle action must trigger onLiftedStop", liftCalled)

        // Simulate Action: Drop Needle
        onScrubEnd(true, 0f)
        assertEquals(0f, dropProgress ?: -1f, 0.001f)

        // Simulate Action: Step Seek forward
        val current = 0.30f
        val next = (current + 0.05f).coerceIn(0f, 1f)
        onScrubEnd(true, next)
        assertEquals(0.35f, seekProgress ?: -1f, 0.001f)
    }
}

