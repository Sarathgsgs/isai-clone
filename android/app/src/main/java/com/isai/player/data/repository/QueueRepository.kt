package com.isai.player.data.repository

import com.isai.player.data.model.Track
import kotlinx.coroutines.flow.Flow

interface QueueRepository {
    fun observeQueue(): Flow<List<Track>>
    suspend fun getQueue(): List<Track>
    suspend fun getQueueTrackIds(): List<Long>
    suspend fun saveQueue(trackIds: List<Long>)
    suspend fun clearQueue()
}

