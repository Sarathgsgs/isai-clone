package com.isai.player.data.repository

import com.isai.player.data.model.Album
import com.isai.player.data.model.AlbumSortOrder
import com.isai.player.data.model.Artist
import com.isai.player.data.model.ArtistSortOrder
import com.isai.player.data.model.Folder
import com.isai.player.data.model.Track
import com.isai.player.data.model.TrackSortOrder
import kotlinx.coroutines.flow.Flow

interface LibraryRepository {
    fun observeTracks(sortBy: TrackSortOrder = TrackSortOrder.TITLE_ASC): Flow<List<Track>>
    fun observeTrackById(id: Long): Flow<Track?>
    suspend fun getTrackById(id: Long): Track?
    suspend fun getTrackByUri(uri: String): Track?
    fun observeTrackCount(): Flow<Int>
    fun searchTracks(query: String): Flow<List<Track>>

    fun observeAlbums(sortBy: AlbumSortOrder = AlbumSortOrder.TITLE_ASC): Flow<List<Album>>
    fun observeTracksForAlbum(album: String, albumArtist: String = ""): Flow<List<Track>>

    fun observeArtists(sortBy: ArtistSortOrder = ArtistSortOrder.NAME_ASC): Flow<List<Artist>>
    fun observeTracksForArtist(artist: String): Flow<List<Track>>

    fun observeFolders(): Flow<List<Folder>>
    fun observeTracksInFolder(folderPath: String): Flow<List<Track>>

    suspend fun insertTracks(tracks: List<Track>)
    suspend fun getAllTrackUris(): List<String>
    suspend fun markTracksMissing(uris: List<String>)
    suspend fun purgeMissingTracks(): Int
}

