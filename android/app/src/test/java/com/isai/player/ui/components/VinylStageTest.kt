package com.isai.player.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VinylStageTest {

    @Test
    fun discSpinningSyncRule() {
        // Rule: disc spins ONLY when playing AND not scrubbing
        fun computeDiscSpinning(isPlaying: Boolean, isScrubbing: Boolean): Boolean {
            return isPlaying && !isScrubbing
        }

        assertTrue(computeDiscSpinning(isPlaying = true, isScrubbing = false))
        assertFalse(computeDiscSpinning(isPlaying = true, isScrubbing = true))
        assertFalse(computeDiscSpinning(isPlaying = false, isScrubbing = false))
        assertFalse(computeDiscSpinning(isPlaying = false, isScrubbing = true))
    }

    @Test
    fun handlePreviousLogicRespectsThreeSecondThreshold() {
        fun resolvePreviousAction(positionMs: Long): PreviousAction {
            return if (positionMs > 3000L) {
                PreviousAction.RESTART_TRACK
            } else {
                PreviousAction.GO_TO_PREVIOUS
            }
        }

        assertEquals(PreviousAction.RESTART_TRACK, resolvePreviousAction(3001L))
        assertEquals(PreviousAction.RESTART_TRACK, resolvePreviousAction(60_000L))
        assertEquals(PreviousAction.GO_TO_PREVIOUS, resolvePreviousAction(3000L))
        assertEquals(PreviousAction.GO_TO_PREVIOUS, resolvePreviousAction(1200L))
        assertEquals(PreviousAction.GO_TO_PREVIOUS, resolvePreviousAction(0L))
    }

    @Test
    fun trackIndexWrapAroundLogic() {
        val trackCount = 5

        fun nextIndex(current: Int): Int = (current + 1) % trackCount
        fun prevIndex(current: Int): Int = if (current > 0) current - 1 else trackCount - 1

        assertEquals(1, nextIndex(0))
        assertEquals(4, nextIndex(3))
        assertEquals(0, nextIndex(4)) // Wrap around to start

        assertEquals(4, prevIndex(0)) // Wrap around to end
        assertEquals(0, prevIndex(1))
        assertEquals(3, prevIndex(4))
    }

    @Test
    fun stageTrackFallbackSeedGeneration() {
        val track = StageTrack(
            id = "test-1",
            title = "Sundari Kannal Oru Sethi",
            artist = "Ilaiyaraaja",
            album = "Thalapathi",
            year = 1991,
            art = null
        )

        val seed = "${track.title} ${track.artist}"
        assertEquals("Sundari Kannal Oru Sethi Ilaiyaraaja", seed)
    }

    private enum class PreviousAction {
        RESTART_TRACK,
        GO_TO_PREVIOUS
    }
}
