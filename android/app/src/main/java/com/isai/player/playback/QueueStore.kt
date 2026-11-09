package com.isai.player.playback

import com.isai.player.data.model.Track
import com.isai.player.data.repository.QueueRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QueueStore @Inject constructor(
    private val queueRepository: QueueRepository
) {
    suspend fun saveQueue(tracks: List<Track>) = withContext(Dispatchers.IO) {
        queueRepository.saveQueue(tracks.map { it.id })
    }

    suspend fun restoreQueue(): List<Track> = withContext(Dispatchers.IO) {
        queueRepository.getQueue()
    }

    suspend fun clearQueue() = withContext(Dispatchers.IO) {
        queueRepository.clearQueue()
    }
}

