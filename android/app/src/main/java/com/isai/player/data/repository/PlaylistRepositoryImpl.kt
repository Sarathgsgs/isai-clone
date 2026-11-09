package com.isai.player.data.repository

import com.isai.player.data.db.dao.PlaylistDao
import com.isai.player.data.db.entity.PlaylistEntity
import com.isai.player.data.db.entity.TrackEntity
import com.isai.player.data.db.projection.PlaylistWithCount
import com.isai.player.data.model.Playlist
import com.isai.player.data.model.Track
import com.isai.player.data.model.toDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistRepositoryImpl @Inject constructor(
    private val playlistDao: PlaylistDao
) : PlaylistRepository {

    override fun observePlaylists(): Flow<List<Playlist>> {
        return playlistDao.observePlaylists().map { list -> list.map(PlaylistWithCount::toDomain) }
    }

    override fun observePlaylistById(id: Long): Flow<Playlist?> {
        return playlistDao.observePlaylistById(id).map { it?.toDomain() }
    }

    override fun observeTracksForPlaylist(playlistId: Long): Flow<List<Track>> {
        return playlistDao.observeTracksForPlaylist(playlistId).map { list -> list.map(TrackEntity::toDomain) }
    }

    override suspend fun createPlaylist(name: String): Long {
        val now = System.currentTimeMillis()
        return playlistDao.insertPlaylist(
            PlaylistEntity(
                name = name.trim(),
                createdAt = now,
                updatedAt = now
            )
        )
    }

    override suspend fun renamePlaylist(playlistId: Long, newName: String) {
        val existing = playlistDao.getPlaylistById(playlistId) ?: return
        playlistDao.updatePlaylist(
            existing.copy(
                name = newName.trim(),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun deletePlaylist(playlistId: Long) {
        playlistDao.deletePlaylist(playlistId)
    }

    override suspend fun addTracksToPlaylist(playlistId: Long, trackIds: List<Long>) {
        playlistDao.addTracksToPlaylist(playlistId, trackIds)
        val existing = playlistDao.getPlaylistById(playlistId)
        if (existing != null) {
            playlistDao.updatePlaylist(existing.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    override suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: Long) {
        playlistDao.removeTrackFromPlaylist(playlistId, trackId)
        val existing = playlistDao.getPlaylistById(playlistId)
        if (existing != null) {
            playlistDao.updatePlaylist(existing.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    override suspend fun reorderPlaylist(playlistId: Long, orderedTrackIds: List<Long>) {
        playlistDao.reorderPlaylist(playlistId, orderedTrackIds)
        val existing = playlistDao.getPlaylistById(playlistId)
        if (existing != null) {
            playlistDao.updatePlaylist(existing.copy(updatedAt = System.currentTimeMillis()))
        }
    }
}

