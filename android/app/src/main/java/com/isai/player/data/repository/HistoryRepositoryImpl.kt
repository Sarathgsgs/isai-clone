package com.isai.player.data.repository

import com.isai.player.data.db.dao.HistoryDao
import com.isai.player.data.db.entity.TrackEntity
import com.isai.player.data.model.Track
import com.isai.player.data.model.toDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepositoryImpl @Inject constructor(
    private val historyDao: HistoryDao
) : HistoryRepository {

    override fun observeRecentlyPlayed(limit: Int): Flow<List<Track>> {
        return historyDao.observeRecentlyPlayed(limit).map { list -> list.map(TrackEntity::toDomain) }
    }

    override suspend fun recordPlay(trackId: Long, playedAt: Long) {
        historyDao.recordPlay(trackId, playedAt)
    }

    override suspend fun clearHistory() {
        historyDao.clearHistory()
    }

    override fun observeHistoryCount(): Flow<Int> {
        return historyDao.observeHistoryCount()
    }
}

