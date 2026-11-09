package com.isai.player.data.db.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.isai.player.data.db.entity.TrackEntity
import com.isai.player.data.model.TrackSortOrder
import kotlinx.coroutines.flow.Flow

data class TrackUriAndModified(
    val uri: String,
    @ColumnInfo(name = "last_modified")
    val lastModified: Long
)

@Dao
interface TrackDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTracks(tracks: List<TrackEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrack(track: TrackEntity)

    @Update
    suspend fun updateTracks(tracks: List<TrackEntity>)

    @Delete
    suspend fun deleteTracks(tracks: List<TrackEntity>)

    @Query("SELECT * FROM tracks WHERE id = :id")
    suspend fun getTrackById(id: Long): TrackEntity?

    @Query("SELECT * FROM tracks WHERE id = :id")
    fun observeTrackById(id: Long): Flow<TrackEntity?>

    @Query("SELECT * FROM tracks WHERE uri = :uri")
    suspend fun getTrackByUri(uri: String): TrackEntity?

    @Query("SELECT uri FROM tracks")
    suspend fun getAllTrackUris(): List<String>

    @Query("SELECT uri, last_modified FROM tracks")
    suspend fun getAllTrackUriAndLastModified(): List<TrackUriAndModified>

    @Query("SELECT COUNT(*) FROM tracks WHERE is_missing = 0")
    fun observeTrackCount(): Flow<Int>

    // Sorting variants
    @Query("SELECT * FROM tracks WHERE is_missing = 0 ORDER BY title_sort COLLATE NOCASE ASC")
    fun observeTracksTitleAsc(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE is_missing = 0 ORDER BY title_sort COLLATE NOCASE DESC")
    fun observeTracksTitleDesc(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE is_missing = 0 ORDER BY artist COLLATE NOCASE ASC, album COLLATE NOCASE ASC, track_no ASC")
    fun observeTracksArtistAsc(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE is_missing = 0 ORDER BY album COLLATE NOCASE ASC, track_no ASC")
    fun observeTracksAlbumAsc(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE is_missing = 0 ORDER BY year DESC, title_sort COLLATE NOCASE ASC")
    fun observeTracksYearDesc(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE is_missing = 0 ORDER BY year ASC, title_sort COLLATE NOCASE ASC")
    fun observeTracksYearAsc(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE is_missing = 0 ORDER BY date_added DESC")
    fun observeTracksDateAddedDesc(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE is_missing = 0 ORDER BY duration_ms DESC")
    fun observeTracksDurationDesc(): Flow<List<TrackEntity>>

    fun observeAllTracks(sortBy: TrackSortOrder): Flow<List<TrackEntity>> = when (sortBy) {
        TrackSortOrder.TITLE_ASC -> observeTracksTitleAsc()
        TrackSortOrder.TITLE_DESC -> observeTracksTitleDesc()
        TrackSortOrder.ARTIST_ASC -> observeTracksArtistAsc()
        TrackSortOrder.ALBUM_ASC -> observeTracksAlbumAsc()
        TrackSortOrder.YEAR_DESC -> observeTracksYearDesc()
        TrackSortOrder.YEAR_ASC -> observeTracksYearAsc()
        TrackSortOrder.DATE_ADDED_DESC -> observeTracksDateAddedDesc()
        TrackSortOrder.DURATION_DESC -> observeTracksDurationDesc()
    }

    // Search
    @Query(
        """
        SELECT * FROM tracks 
        WHERE is_missing = 0 AND (
            title LIKE '%' || :query || '%' OR 
            artist LIKE '%' || :query || '%' OR 
            album LIKE '%' || :query || '%'
        )
        ORDER BY title_sort COLLATE NOCASE ASC
        """
    )
    fun searchTracks(query: String): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE is_missing = 0 ORDER BY title_sort COLLATE NOCASE ASC LIMIT :limit OFFSET :offset")
    suspend fun getTracksPaged(limit: Int, offset: Int): List<TrackEntity>

    @Query("SELECT * FROM tracks WHERE is_missing = 0 ORDER BY RANDOM() LIMIT :limit")
    suspend fun getRandomTracks(limit: Int): List<TrackEntity>

    @Query("SELECT * FROM tracks WHERE is_missing = 0 AND album = :album ORDER BY track_no ASC")
    suspend fun getTracksByAlbum(album: String): List<TrackEntity>

    @Query("SELECT * FROM tracks WHERE is_missing = 0 AND artist = :artist ORDER BY album COLLATE NOCASE ASC, track_no ASC")
    suspend fun getTracksByArtist(artist: String): List<TrackEntity>

    // Missing tracks handling
    @Query("UPDATE tracks SET is_missing = 1 WHERE uri IN (:uris)")
    suspend fun markTracksMissing(uris: List<String>)

    @Query("UPDATE tracks SET is_missing = 0 WHERE uri = :uri")
    suspend fun markTrackFound(uri: String)

    @Query("DELETE FROM tracks WHERE is_missing = 1")
    suspend fun purgeMissingTracks(): Int
}

