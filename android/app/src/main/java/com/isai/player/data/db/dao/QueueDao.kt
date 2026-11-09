package com.isai.player.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.isai.player.data.db.entity.QueueItemEntity
import com.isai.player.data.db.entity.TrackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QueueDao {

    @Query(
        """
        SELECT tracks.* FROM tracks
        INNER JOIN queue_items ON tracks.id = queue_items.track_id
        WHERE tracks.is_missing = 0
        ORDER BY queue_items.position ASC
        """
    )
    fun observeQueueTracks(): Flow<List<TrackEntity>>

    @Query(
        """
        SELECT tracks.* FROM tracks
        INNER JOIN queue_items ON tracks.id = queue_items.track_id
        WHERE tracks.is_missing = 0
        ORDER BY queue_items.position ASC
        """
    )
    suspend fun getQueueTracks(): List<TrackEntity>

    @Query("SELECT track_id FROM queue_items ORDER BY position ASC")
    suspend fun getQueueTrackIds(): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQueueItems(items: List<QueueItemEntity>)

    @Query("DELETE FROM queue_items")
    suspend fun clearQueue()

    @Transaction
    suspend fun saveQueue(trackIds: List<Long>) {
        clearQueue()
        val items = trackIds.mapIndexed { index, id ->
            QueueItemEntity(position = index, trackId = id)
        }
        insertQueueItems(items)
    }
}

