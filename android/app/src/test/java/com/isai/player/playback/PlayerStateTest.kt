package com.isai.player.playback

import com.isai.player.data.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerStateTest {

    @Test
    fun progressCalculations() {
        val emptyState = PlayerState(currentPositionMs = 0L, durationMs = 0L)
        assertEquals(0f, emptyState.progress, 0.001f)

        val halfWayState = PlayerState(currentPositionMs = 120_000L, durationMs = 240_000L)
        assertEquals(0.5f, halfWayState.progress, 0.001f)

        val overshootState = PlayerState(currentPositionMs = 300_000L, durationMs = 240_000L)
        assertEquals(1.0f, overshootState.progress, 0.001f)

        val negativeState = PlayerState(currentPositionMs = -5000L, durationMs = 240_000L)
        assertEquals(0.0f, negativeState.progress, 0.001f)
    }

    @Test
    fun repeatModeCycleSequence() {
        assertEquals(RepeatMode.ALL, RepeatMode.OFF.next())
        assertEquals(RepeatMode.ONE, RepeatMode.ALL.next())
        assertEquals(RepeatMode.OFF, RepeatMode.ONE.next())
    }

    @Test
    fun hasTrackFlagReflectsPresenceOfCurrentTrack() {
        val noTrackState = PlayerState(currentTrack = null)
        assertFalse(noTrackState.hasTrack)

        val sampleTrack = Track(
            id = 1L,
            uri = "uri:1",
            title = "Sundari",
            titleSort = "sundari",
            artist = "Ilaiyaraaja",
            albumArtist = "Ilaiyaraaja",
            album = "Thalapathi",
            year = 1991,
            trackNo = 1,
            discNo = 1,
            durationMs = 240000L,
            mimeType = "audio/mpeg",
            sizeBytes = 5000000L,
            lastModified = 0L,
            artPath = null,
            folderPath = "Music",
            dateAdded = 0L
        )

        val withTrackState = PlayerState(currentTrack = sampleTrack)
        assertTrue(withTrackState.hasTrack)
    }
}

