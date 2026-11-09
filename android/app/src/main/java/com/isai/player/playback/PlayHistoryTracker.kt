package com.isai.player.playback

import com.isai.player.data.model.Track
import com.isai.player.data.repository.HistoryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayHistoryTracker @Inject constructor(
    private val historyRepository: HistoryRepository
) {
    private var currentTrackId: Long? = null
    private var currentDurationMs: Long = 0L
    private var accumulatedPlayedMs: Long = 0L
    private var lastPositionMs: Long = 0L
    private var hasLoggedCurrentTrack: Boolean = false

    fun onTrackChanged(track: Track?) {
        currentTrackId = track?.id
        currentDurationMs = track?.durationMs ?: 0L
        accumulatedPlayedMs = 0L
        lastPositionMs = 0L
        hasLoggedCurrentTrack = false
    }

    fun onPositionUpdate(positionMs: Long, isPlaying: Boolean, scope: CoroutineScope) {
        val trackId = currentTrackId ?: return
        if (hasLoggedCurrentTrack || !isPlaying) return

        if (positionMs > lastPositionMs) {
            val delta = positionMs - lastPositionMs
            if (delta in 1..2000) { // Reasonable delta, prevents seeks from falsely logging
                accumulatedPlayedMs += delta
            }
        }
        lastPositionMs = positionMs

        val thresholdMs = if (currentDurationMs in 1..30_000L) {
            currentDurationMs / 2
        } else {
            30_000L
        }

        if (accumulatedPlayedMs >= thresholdMs) {
            hasLoggedCurrentTrack = true
            scope.launch(Dispatchers.IO) {
                historyRepository.recordPlay(trackId)
            }
        }
    }

    fun isLoggedForCurrentTrack(): Boolean = hasLoggedCurrentTrack
}

