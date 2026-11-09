package com.isai.player.data.repository

import com.isai.player.data.db.dao.FavoriteDao
import com.isai.player.data.db.entity.FavoriteEntity
import com.isai.player.data.db.entity.TrackEntity
import com.isai.player.data.model.Track
import com.isai.player.data.model.toDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoritesRepositoryImpl @Inject constructor(
    private val favoriteDao: FavoriteDao
) : FavoritesRepository {

    override fun observeFavoriteTracks(): Flow<List<Track>> {
        return favoriteDao.observeFavoriteTracks().map { list -> list.map(TrackEntity::toDomain) }
    }

    override fun observeFavoriteTrackIds(): Flow<List<Long>> {
        return favoriteDao.observeFavoriteTrackIds()
    }

    override fun observeIsFavorite(trackId: Long): Flow<Boolean> {
        return favoriteDao.observeIsFavorite(trackId)
    }

    override suspend fun isFavorite(trackId: Long): Boolean {
        return favoriteDao.isFavorite(trackId)
    }

    override suspend fun toggleFavorite(trackId: Long) {
        favoriteDao.toggleFavorite(trackId)
    }

    override suspend fun addFavorite(trackId: Long) {
        favoriteDao.addFavorite(FavoriteEntity(trackId = trackId, addedAt = System.currentTimeMillis()))
    }

    override suspend fun removeFavorite(trackId: Long) {
        favoriteDao.removeFavorite(trackId)
    }

    override fun observeFavoriteCount(): Flow<Int> {
        return favoriteDao.observeFavoriteCount()
    }
}

