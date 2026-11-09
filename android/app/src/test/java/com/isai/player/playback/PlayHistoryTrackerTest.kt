package com.isai.player.playback

import com.isai.player.data.model.Track
import com.isai.player.data.repository.HistoryRepository
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PlayHistoryTrackerTest {

    private lateinit var historyRepository: HistoryRepository
    private lateinit var tracker: PlayHistoryTracker
    private val scope = CoroutineScope(Dispatchers.Unconfined)

    private fun createTrack(id: Long, durationMs: Long): Track {
        return Track(
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
    }

    @Before
    fun setup() {
        historyRepository = mockk(relaxed = true)
        tracker = PlayHistoryTracker(historyRepository)
    }

    @Test
    fun standardTrackRequiresThirtySecondsPlaybackBeforeLogging() {
        val track = createTrack(id = 100L, durationMs = 240_000L) // 4 minutes
        tracker.onTrackChanged(track)

        assertFalse(tracker.isLoggedForCurrentTrack())

        // Play for 10 seconds in 1-second ticks
        for (sec in 1..10) {
            tracker.onPositionUpdate(positionMs = sec * 1000L, isPlaying = true, scope = scope)
        }
        assertFalse(tracker.isLoggedForCurrentTrack())

        // Play up to 29 seconds
        for (sec in 11..29) {
            tracker.onPositionUpdate(positionMs = sec * 1000L, isPlaying = true, scope = scope)
        }
        assertFalse(tracker.isLoggedForCurrentTrack())

        // Tick 30: reaches 30 seconds threshold -> Logged!
        tracker.onPositionUpdate(positionMs = 30_000L, isPlaying = true, scope = scope)
        assertTrue(tracker.isLoggedForCurrentTrack())

        coVerify(exactly = 1) { historyRepository.recordPlay(100L) }

        // Subsequent position updates should not trigger another recordPlay
        for (sec in 31..60) {
            tracker.onPositionUpdate(positionMs = sec * 1000L, isPlaying = true, scope = scope)
        }
        coVerify(exactly = 1) { historyRepository.recordPlay(100L) }
    }

    @Test
    fun shortTrackUsesFiftyPercentDurationThreshold() {
        val shortTrack = createTrack(id = 200L, durationMs = 20_000L) // 20 seconds track
        tracker.onTrackChanged(shortTrack)

        // Threshold = 10 seconds (50% of 20s)
        for (sec in 1..9) {
            tracker.onPositionUpdate(positionMs = sec * 1000L, isPlaying = true, scope = scope)
        }
        assertFalse(tracker.isLoggedForCurrentTrack())

        tracker.onPositionUpdate(positionMs = 10_000L, isPlaying = true, scope = scope)
        assertTrue(tracker.isLoggedForCurrentTrack())

        coVerify(exactly = 1) { historyRepository.recordPlay(200L) }
    }

    @Test
    fun changingTrackResetsLoggerState() {
        val track1 = createTrack(id = 1L, durationMs = 180_000L)
        val track2 = createTrack(id = 2L, durationMs = 180_000L)

        tracker.onTrackChanged(track1)
        for (sec in 1..30) {
            tracker.onPositionUpdate(positionMs = sec * 1000L, isPlaying = true, scope = scope)
        }
        assertTrue(tracker.isLoggedForCurrentTrack())

        // Change track
        tracker.onTrackChanged(track2)
        assertFalse(tracker.isLoggedForCurrentTrack())
    }
}

