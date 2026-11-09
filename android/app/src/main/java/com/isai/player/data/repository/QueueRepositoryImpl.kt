package com.isai.player.data.repository

import com.isai.player.data.db.dao.QueueDao
import com.isai.player.data.db.entity.TrackEntity
import com.isai.player.data.model.Track
import com.isai.player.data.model.toDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QueueRepositoryImpl @Inject constructor(
    private val queueDao: QueueDao
) : QueueRepository {

    override fun observeQueue(): Flow<List<Track>> {
        return queueDao.observeQueueTracks().map { list -> list.map(TrackEntity::toDomain) }
    }

    override suspend fun getQueue(): List<Track> {
        return queueDao.getQueueTracks().map(TrackEntity::toDomain)
    }

    override suspend fun getQueueTrackIds(): List<Long> {
        return queueDao.getQueueTrackIds()
    }

    override suspend fun saveQueue(trackIds: List<Long>) {
        queueDao.saveQueue(trackIds)
    }

    override suspend fun clearQueue() {
        queueDao.clearQueue()
    }
}

