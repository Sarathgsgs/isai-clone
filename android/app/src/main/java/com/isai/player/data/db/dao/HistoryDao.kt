package com.isai.player.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.isai.player.data.db.entity.PlayHistoryEntity
import com.isai.player.data.db.entity.TrackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistoryEntry(entry: PlayHistoryEntity): Long

    @Query(
        """
        SELECT tracks.* FROM tracks
        INNER JOIN (
            SELECT track_id, MAX(played_at) AS latest_play
            FROM play_history
            GROUP BY track_id
            ORDER BY latest_play DESC
            LIMIT :limit
        ) AS recent ON tracks.id = recent.track_id
        WHERE tracks.is_missing = 0
        ORDER BY recent.latest_play DESC
        """
    )
    fun observeRecentlyPlayed(limit: Int = 30): Flow<List<TrackEntity>>

    @Query(
        """
        SELECT tracks.* FROM tracks
        INNER JOIN (
            SELECT track_id, MAX(played_at) AS latest_play
            FROM play_history
            GROUP BY track_id
            ORDER BY latest_play DESC
            LIMIT :limit
        ) AS recent ON tracks.id = recent.track_id
        WHERE tracks.is_missing = 0
        ORDER BY recent.latest_play DESC
        """
    )
    suspend fun getRecentlyPlayedSync(limit: Int = 20): List<TrackEntity>

    @Query("DELETE FROM play_history")
    suspend fun clearHistory()

    @Query("SELECT COUNT(*) FROM play_history")
    fun observeHistoryCount(): Flow<Int>

    @Transaction
    suspend fun recordPlay(trackId: Long, playedAt: Long = System.currentTimeMillis()) {
        insertHistoryEntry(PlayHistoryEntity(trackId = trackId, playedAt = playedAt))
    }
}

