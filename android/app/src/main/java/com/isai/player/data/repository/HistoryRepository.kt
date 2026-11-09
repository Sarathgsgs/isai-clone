package com.isai.player.data.repository

import com.isai.player.data.model.Track
import kotlinx.coroutines.flow.Flow

interface HistoryRepository {
    fun observeRecentlyPlayed(limit: Int = 30): Flow<List<Track>>
    suspend fun recordPlay(trackId: Long, playedAt: Long = System.currentTimeMillis())
    suspend fun clearHistory()
    fun observeHistoryCount(): Flow<Int>
}

