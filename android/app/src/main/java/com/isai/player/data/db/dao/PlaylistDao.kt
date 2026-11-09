package com.isai.player.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.isai.player.data.db.entity.PlaylistEntity
import com.isai.player.data.db.entity.PlaylistTrackEntity
import com.isai.player.data.db.entity.TrackEntity
import com.isai.player.data.db.projection.PlaylistWithCount
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Update
    suspend fun updatePlaylist(playlist: PlaylistEntity)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)

    @Query("SELECT * FROM playlists WHERE id = :playlistId")
    fun observePlaylistById(playlistId: Long): Flow<PlaylistEntity?>

    @Query("SELECT * FROM playlists WHERE id = :playlistId")
    suspend fun getPlaylistById(playlistId: Long): PlaylistEntity?

    @Query(
        """
        SELECT 
            playlists.*,
            COUNT(playlist_tracks.track_id) AS track_count
        FROM playlists
        LEFT JOIN playlist_tracks ON playlists.id = playlist_tracks.playlist_id
        GROUP BY playlists.id
        ORDER BY playlists.name COLLATE NOCASE ASC
        """
    )
    fun observePlaylists(): Flow<List<PlaylistWithCount>>

    @Query(
        """
        SELECT 
            playlists.*,
            COUNT(playlist_tracks.track_id) AS track_count
        FROM playlists
        LEFT JOIN playlist_tracks ON playlists.id = playlist_tracks.playlist_id
        GROUP BY playlists.id
        ORDER BY playlists.name COLLATE NOCASE ASC
        """
    )
    suspend fun getAllPlaylistsSync(): List<PlaylistWithCount>

    @Query(
        """
        SELECT tracks.* FROM tracks
        INNER JOIN playlist_tracks ON tracks.id = playlist_tracks.track_id
        WHERE playlist_tracks.playlist_id = :playlistId AND tracks.is_missing = 0
        ORDER BY playlist_tracks.position ASC
        """
    )
    suspend fun getTracksForPlaylistSync(playlistId: Long): List<TrackEntity>

    @Query(
        """
        SELECT tracks.* FROM tracks
        INNER JOIN playlist_tracks ON tracks.id = playlist_tracks.track_id
        WHERE playlist_tracks.playlist_id = :playlistId AND tracks.is_missing = 0
        ORDER BY playlist_tracks.position ASC
        """
    )
    fun observeTracksForPlaylist(playlistId: Long): Flow<List<TrackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistTracks(playlistTracks: List<PlaylistTrackEntity>)

    @Query("DELETE FROM playlist_tracks WHERE playlist_id = :playlistId AND track_id = :trackId")
    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: Long)

    @Query("DELETE FROM playlist_tracks WHERE playlist_id = :playlistId")
    suspend fun clearPlaylistTracks(playlistId: Long)

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM playlist_tracks WHERE playlist_id = :playlistId")
    suspend fun getNextPositionForPlaylist(playlistId: Long): Int

    @Transaction
    suspend fun addTracksToPlaylist(playlistId: Long, trackIds: List<Long>) {
        val startPos = getNextPositionForPlaylist(playlistId)
        val now = System.currentTimeMillis()
        val entities = trackIds.mapIndexed { index, trackId ->
            PlaylistTrackEntity(
                playlistId = playlistId,
                trackId = trackId,
                position = startPos + index,
                addedAt = now
            )
        }
        insertPlaylistTracks(entities)
    }

    @Transaction
    suspend fun reorderPlaylist(playlistId: Long, orderedTrackIds: List<Long>) {
        clearPlaylistTracks(playlistId)
        val now = System.currentTimeMillis()
        val entities = orderedTrackIds.mapIndexed { index, trackId ->
            PlaylistTrackEntity(
                playlistId = playlistId,
                trackId = trackId,
                position = index,
                addedAt = now
            )
        }
        insertPlaylistTracks(entities)
    }
}

