package com.isai.player.playback

import androidx.compose.ui.geometry.Offset
import com.isai.player.data.model.Track
import com.isai.player.ui.components.TonearmGeometry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackHardeningTest {

    private fun createSampleTrack(id: Long = 1L, durationMs: Long = 180000L): Track = Track(
        id = id,
        uri = "uri:$id",
        title = "Track $id",
        titleSort = "track $id",
        artist = "Artist",
        albumArtist = "Artist",
        album = "Album",
        year = 2024,
        trackNo = 1,
        discNo = 1,
        durationMs = durationMs,
        mimeType = "audio/mpeg",
        sizeBytes = 1000L,
        lastModified = 0L,
        artPath = null,
        folderPath = "Music",
        dateAdded = 0L
    )

    @Test
    fun zeroDurationReturnsZeroProgressWithoutNaN() {
        val state = PlayerState(
            currentTrack = createSampleTrack(1L, durationMs = 0L),
            durationMs = 0L,
            currentPositionMs = 5000L
        )

        assertEquals(0f, state.progress, 0.0001f)
        assertFalse(state.progress.isNaN())
        assertFalse(state.progress.isInfinite())
    }

    @Test
    fun negativeDurationReturnsZeroProgressSafely() {
        val state = PlayerState(
            currentTrack = createSampleTrack(1L, durationMs = -500L),
            durationMs = -500L,
            currentPositionMs = 1000L
        )

        assertEquals(0f, state.progress, 0.0001f)
        assertFalse(state.progress.isNaN())
    }

    @Test
    fun tonearmGeometryHandlesNaNProgressSafely() {
        val geometry = TonearmGeometry.forDisc(
            center = Offset(200f, 200f),
            discRadius = 150f
        )

        val angleFromNaN = geometry.trackAngle(Float.NaN)
        assertFalse(angleFromNaN.isNaN())
        assertFalse(angleFromNaN.isInfinite())
        assertEquals(geometry.alphaOuter, angleFromNaN, 0.001f)

        val angleFromInfinity = geometry.trackAngle(Float.POSITIVE_INFINITY)
        assertFalse(angleFromInfinity.isNaN())
        assertFalse(angleFromInfinity.isInfinite())
    }

    @Test
    fun tonearmGeometryHandlesNaNAngleSafely() {
        val geometry = TonearmGeometry.forDisc(
            center = Offset(200f, 200f),
            discRadius = 150f
        )

        val progressFromNaN = geometry.progressFromAngle(Float.NaN)
        assertEquals(0f, progressFromNaN, 0.0001f)
        assertFalse(progressFromNaN.isNaN())
    }

    @Test
    fun normalProgressClampingBetweenZeroAndOne() {
        val stateUnder = PlayerState(
            currentTrack = createSampleTrack(1L, durationMs = 10000L),
            durationMs = 10000L,
            currentPositionMs = -50L
        )
        assertEquals(0f, stateUnder.progress, 0.0001f)

        val stateOver = PlayerState(
            currentTrack = createSampleTrack(1L, durationMs = 10000L),
            durationMs = 10000L,
            currentPositionMs = 25000L
        )
        assertEquals(1f, stateOver.progress, 0.0001f)
    }
}

