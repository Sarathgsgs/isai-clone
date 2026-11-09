package com.isai.player.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import com.isai.player.data.db.entity.TrackEntity
import com.isai.player.data.db.projection.FolderProjection
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {

    @Query(
        """
        SELECT 
            folder_path,
            COUNT(*) AS track_count
        FROM tracks
        WHERE is_missing = 0
        GROUP BY folder_path
        ORDER BY folder_path COLLATE NOCASE ASC
        """
    )
    fun observeFolders(): Flow<List<FolderProjection>>

    @Query(
        """
        SELECT * FROM tracks
        WHERE is_missing = 0 AND folder_path = :folderPath
        ORDER BY title_sort COLLATE NOCASE ASC
        """
    )
    fun observeTracksInFolder(folderPath: String): Flow<List<TrackEntity>>
}

