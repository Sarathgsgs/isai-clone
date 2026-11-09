package com.isai.player.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.isai.player.data.db.entity.FavoriteEntity
import com.isai.player.data.db.entity.TrackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE track_id = :trackId")
    suspend fun removeFavorite(trackId: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE track_id = :trackId)")
    fun observeIsFavorite(trackId: Long): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE track_id = :trackId)")
    suspend fun isFavorite(trackId: Long): Boolean

    @Query("SELECT track_id FROM favorites")
    fun observeFavoriteTrackIds(): Flow<List<Long>>

    @Query(
        """
        SELECT tracks.* FROM tracks
        INNER JOIN favorites ON tracks.id = favorites.track_id
        WHERE tracks.is_missing = 0
        ORDER BY favorites.added_at DESC
        """
    )
    fun observeFavoriteTracks(): Flow<List<TrackEntity>>

    @Query("SELECT COUNT(*) FROM favorites")
    fun observeFavoriteCount(): Flow<Int>

    @Query(
        """
        SELECT tracks.* FROM tracks
        INNER JOIN favorites ON tracks.id = favorites.track_id
        WHERE tracks.is_missing = 0
        ORDER BY favorites.added_at DESC
        LIMIT :limit
        """
    )
    suspend fun getFavoriteTracksSync(limit: Int = 200): List<TrackEntity>

    @Transaction
    suspend fun toggleFavorite(trackId: Long) {
        if (isFavorite(trackId)) {
            removeFavorite(trackId)
        } else {
            addFavorite(FavoriteEntity(trackId = trackId, addedAt = System.currentTimeMillis()))
        }
    }
}

