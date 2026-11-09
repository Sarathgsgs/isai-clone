package com.isai.player.data.repository

import com.isai.player.data.db.dao.AlbumDao
import com.isai.player.data.db.dao.ArtistDao
import com.isai.player.data.db.dao.FolderDao
import com.isai.player.data.db.dao.TrackDao
import com.isai.player.data.db.entity.TrackEntity
import com.isai.player.data.db.projection.AlbumProjection
import com.isai.player.data.db.projection.ArtistProjection
import com.isai.player.data.db.projection.FolderProjection
import com.isai.player.data.model.Album
import com.isai.player.data.model.AlbumSortOrder
import com.isai.player.data.model.Artist
import com.isai.player.data.model.ArtistSortOrder
import com.isai.player.data.model.Folder
import com.isai.player.data.model.Track
import com.isai.player.data.model.TrackSortOrder
import com.isai.player.data.model.toDomain
import com.isai.player.data.model.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LibraryRepositoryImpl @Inject constructor(
    private val trackDao: TrackDao,
    private val albumDao: AlbumDao,
    private val artistDao: ArtistDao,
    private val folderDao: FolderDao
) : LibraryRepository {

    override fun observeTracks(sortBy: TrackSortOrder): Flow<List<Track>> {
        return trackDao.observeAllTracks(sortBy).map { list -> list.map(TrackEntity::toDomain) }
    }

    override fun observeTrackById(id: Long): Flow<Track?> {
        return trackDao.observeTrackById(id).map { it?.toDomain() }
    }

    override suspend fun getTrackById(id: Long): Track? {
        return trackDao.getTrackById(id)?.toDomain()
    }

    override suspend fun getTrackByUri(uri: String): Track? {
        return trackDao.getTrackByUri(uri)?.toDomain()
    }

    override fun observeTrackCount(): Flow<Int> {
        return trackDao.observeTrackCount()
    }

    override fun searchTracks(query: String): Flow<List<Track>> {
        return trackDao.searchTracks(query).map { list -> list.map(TrackEntity::toDomain) }
    }

    override fun observeAlbums(sortBy: AlbumSortOrder): Flow<List<Album>> {
        return albumDao.observeAlbums(sortBy).map { list -> list.map(AlbumProjection::toDomain) }
    }

    override fun observeTracksForAlbum(album: String, albumArtist: String): Flow<List<Track>> {
        val flow = if (albumArtist.isBlank()) {
            albumDao.observeTracksForAlbum(album)
        } else {
            albumDao.observeTracksForAlbum(album, albumArtist)
        }
        return flow.map { list -> list.map(TrackEntity::toDomain) }
    }

    override fun observeArtists(sortBy: ArtistSortOrder): Flow<List<Artist>> {
        return artistDao.observeArtists(sortBy).map { list -> list.map(ArtistProjection::toDomain) }
    }

    override fun observeTracksForArtist(artist: String): Flow<List<Track>> {
        return artistDao.observeTracksForArtist(artist).map { list -> list.map(TrackEntity::toDomain) }
    }

    override fun observeFolders(): Flow<List<Folder>> {
        return folderDao.observeFolders().map { list -> list.map(FolderProjection::toDomain) }
    }

    override fun observeTracksInFolder(folderPath: String): Flow<List<Track>> {
        return folderDao.observeTracksInFolder(folderPath).map { list -> list.map(TrackEntity::toDomain) }
    }

    override suspend fun insertTracks(tracks: List<Track>) {
        trackDao.insertTracks(tracks.map(Track::toEntity))
    }

    override suspend fun getAllTrackUris(): List<String> {
        return trackDao.getAllTrackUris()
    }

    override suspend fun markTracksMissing(uris: List<String>) {
        trackDao.markTracksMissing(uris)
    }

    override suspend fun purgeMissingTracks(): Int {
        return trackDao.purgeMissingTracks()
    }
}

