package com.isai.player.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import com.isai.player.data.db.entity.TrackEntity
import com.isai.player.data.db.projection.ArtistProjection
import com.isai.player.data.model.ArtistSortOrder
import kotlinx.coroutines.flow.Flow

@Dao
interface ArtistDao {

    @Query(
        """
        SELECT 
            artist,
            COUNT(*) AS track_count,
            COUNT(DISTINCT album) AS album_count,
            MAX(art_path) AS sample_art_path
        FROM tracks
        WHERE is_missing = 0 AND artist != ''
        GROUP BY artist
        ORDER BY artist COLLATE NOCASE ASC
        """
    )
    fun observeArtistsNameAsc(): Flow<List<ArtistProjection>>

    @Query(
        """
        SELECT 
            artist,
            COUNT(*) AS track_count,
            COUNT(DISTINCT album) AS album_count,
            MAX(art_path) AS sample_art_path
        FROM tracks
        WHERE is_missing = 0 AND artist != ''
        GROUP BY artist
        ORDER BY track_count DESC, artist COLLATE NOCASE ASC
        """
    )
    fun observeArtistsTrackCountDesc(): Flow<List<ArtistProjection>>

    fun observeArtists(sortBy: ArtistSortOrder): Flow<List<ArtistProjection>> = when (sortBy) {
        ArtistSortOrder.NAME_ASC -> observeArtistsNameAsc()
        ArtistSortOrder.TRACK_COUNT_DESC -> observeArtistsTrackCountDesc()
    }

    @Query(
        """
        SELECT * FROM tracks
        WHERE is_missing = 0 AND (artist = :artist OR album_artist = :artist)
        ORDER BY year DESC, album COLLATE NOCASE ASC, track_no ASC
        """
    )
    fun observeTracksForArtist(artist: String): Flow<List<TrackEntity>>

    @Query(
        """
        SELECT 
            artist,
            COUNT(*) AS track_count,
            COUNT(DISTINCT album) AS album_count,
            MAX(art_path) AS sample_art_path
        FROM tracks
        WHERE is_missing = 0 AND artist != ''
        GROUP BY artist
        ORDER BY artist COLLATE NOCASE ASC
        LIMIT :limit OFFSET :offset
        """
    )
    suspend fun getArtistsPaged(limit: Int, offset: Int): List<ArtistProjection>

    @Query(
        """
        SELECT * FROM tracks
        WHERE is_missing = 0 AND (artist = :artist OR album_artist = :artist)
        ORDER BY year DESC, album COLLATE NOCASE ASC, track_no ASC
        """
    )
    suspend fun getTracksForArtistSync(artist: String): List<TrackEntity>
}

