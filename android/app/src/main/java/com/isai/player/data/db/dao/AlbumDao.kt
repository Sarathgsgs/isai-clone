package com.isai.player.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import com.isai.player.data.db.entity.TrackEntity
import com.isai.player.data.db.projection.AlbumProjection
import com.isai.player.data.model.AlbumSortOrder
import kotlinx.coroutines.flow.Flow

@Dao
interface AlbumDao {

    @Query(
        """
        SELECT 
            album,
            album_artist,
            COUNT(*) AS track_count,
            MIN(year) AS min_year,
            MAX(art_path) AS art_path
        FROM tracks
        WHERE is_missing = 0 AND album != ''
        GROUP BY album, album_artist
        ORDER BY album COLLATE NOCASE ASC
        """
    )
    fun observeAlbumsTitleAsc(): Flow<List<AlbumProjection>>

    @Query(
        """
        SELECT 
            album,
            album_artist,
            COUNT(*) AS track_count,
            MIN(year) AS min_year,
            MAX(art_path) AS art_path
        FROM tracks
        WHERE is_missing = 0 AND album != ''
        GROUP BY album, album_artist
        ORDER BY album_artist COLLATE NOCASE ASC, album COLLATE NOCASE ASC
        """
    )
    fun observeAlbumsArtistAsc(): Flow<List<AlbumProjection>>

    @Query(
        """
        SELECT 
            album,
            album_artist,
            COUNT(*) AS track_count,
            MIN(year) AS min_year,
            MAX(art_path) AS art_path
        FROM tracks
        WHERE is_missing = 0 AND album != ''
        GROUP BY album, album_artist
        ORDER BY min_year DESC, album COLLATE NOCASE ASC
        """
    )
    fun observeAlbumsYearDesc(): Flow<List<AlbumProjection>>

    @Query(
        """
        SELECT 
            album,
            album_artist,
            COUNT(*) AS track_count,
            MIN(year) AS min_year,
            MAX(art_path) AS art_path
        FROM tracks
        WHERE is_missing = 0 AND album != ''
        GROUP BY album, album_artist
        ORDER BY min_year ASC, album COLLATE NOCASE ASC
        """
    )
    fun observeAlbumsYearAsc(): Flow<List<AlbumProjection>>

    fun observeAlbums(sortBy: AlbumSortOrder): Flow<List<AlbumProjection>> = when (sortBy) {
        AlbumSortOrder.TITLE_ASC -> observeAlbumsTitleAsc()
        AlbumSortOrder.ARTIST_ASC -> observeAlbumsArtistAsc()
        AlbumSortOrder.YEAR_DESC -> observeAlbumsYearDesc()
        AlbumSortOrder.YEAR_ASC -> observeAlbumsYearAsc()
    }

    @Query(
        """
        SELECT * FROM tracks
        WHERE is_missing = 0 AND album = :album AND album_artist = :albumArtist
        ORDER BY disc_no ASC, track_no ASC, title_sort COLLATE NOCASE ASC
        """
    )
    fun observeTracksForAlbum(album: String, albumArtist: String): Flow<List<TrackEntity>>

    @Query(
        """
        SELECT * FROM tracks
        WHERE is_missing = 0 AND album = :album
        ORDER BY disc_no ASC, track_no ASC, title_sort COLLATE NOCASE ASC
        """
    )
    fun observeTracksForAlbum(album: String): Flow<List<TrackEntity>>

    @Query(
        """
        SELECT 
            album,
            album_artist,
            COUNT(*) AS track_count,
            MIN(year) AS min_year,
            MAX(art_path) AS art_path
        FROM tracks
        WHERE is_missing = 0 AND album != ''
        GROUP BY album, album_artist
        ORDER BY album COLLATE NOCASE ASC
        LIMIT :limit OFFSET :offset
        """
    )
    suspend fun getAlbumsPaged(limit: Int, offset: Int): List<AlbumProjection>

    @Query(
        """
        SELECT * FROM tracks
        WHERE is_missing = 0 AND album = :album
        ORDER BY disc_no ASC, track_no ASC, title_sort COLLATE NOCASE ASC
        """
    )
    suspend fun getTracksForAlbumSync(album: String): List<TrackEntity>
}

