package com.isai.player.data.repository

import android.net.Uri
import kotlinx.coroutines.flow.Flow

/**
 * State representing audio waveform peak availability for the current track.
 */
sealed interface WaveformState {
    object Loading : WaveformState
    data class Ready(val peaks: ByteArray) : WaveformState {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as Ready
            return peaks.contentEquals(other.peaks)
        }

        override fun hashCode(): Int {
            return peaks.contentHashCode()
        }
    }
    object Unavailable : WaveformState
}

interface WaveformRepository {
    fun observeWaveform(
        trackId: Long,
        durationMs: Long,
        trackUri: Uri,
        lastModified: Long
    ): Flow<WaveformState>

    fun prefetchWaveform(
        trackId: Long,
        durationMs: Long,
        trackUri: Uri,
        lastModified: Long
    )

    fun observeWaveformCount(): Flow<Int>
    suspend fun getWaveformCount(): Int
    suspend fun clearWaveformCache()
}

