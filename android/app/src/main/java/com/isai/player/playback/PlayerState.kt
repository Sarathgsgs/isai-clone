package com.isai.player.playback

import androidx.compose.runtime.Immutable
import androidx.media3.common.Player
import com.isai.player.data.model.Track

enum class RepeatMode {
    OFF,
    ALL,
    ONE;

    fun next(): RepeatMode = when (this) {
        OFF -> ALL
        ALL -> ONE
        ONE -> OFF
    }
}

@Immutable
data class PlayerState(
    val currentTrack: Track? = null,
    val isPlaying: Boolean = false,
    val playbackState: Int = Player.STATE_IDLE,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
    val shuffleModeEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val queue: List<Track> = emptyList(),
    val currentQueueIndex: Int = -1,
    val isConnected: Boolean = false,
    val playbackErrorMessage: String? = null
) {
    val progress: Float
        get() = if (durationMs > 0L) {
            (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

    val hasTrack: Boolean
        get() = currentTrack != null

    val isBuffering: Boolean
        get() = playbackState == Player.STATE_BUFFERING
}

