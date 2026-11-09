package com.isai.player.data.repository

import com.isai.player.data.model.Track
import kotlinx.coroutines.flow.Flow

interface FavoritesRepository {
    fun observeFavoriteTracks(): Flow<List<Track>>
    fun observeFavoriteTrackIds(): Flow<List<Long>>
    fun observeIsFavorite(trackId: Long): Flow<Boolean>
    suspend fun isFavorite(trackId: Long): Boolean
    suspend fun toggleFavorite(trackId: Long)
    suspend fun addFavorite(trackId: Long)
    suspend fun removeFavorite(trackId: Long)
    fun observeFavoriteCount(): Flow<Int>
}

