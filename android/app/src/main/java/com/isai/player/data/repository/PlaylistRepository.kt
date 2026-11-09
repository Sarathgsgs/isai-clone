package com.isai.player.data.repository

import com.isai.player.data.model.Playlist
import com.isai.player.data.model.Track
import kotlinx.coroutines.flow.Flow

interface PlaylistRepository {
    fun observePlaylists(): Flow<List<Playlist>>
    fun observePlaylistById(id: Long): Flow<Playlist?>
    fun observeTracksForPlaylist(playlistId: Long): Flow<List<Track>>
    suspend fun createPlaylist(name: String): Long
    suspend fun renamePlaylist(playlistId: Long, newName: String)
    suspend fun deletePlaylist(playlistId: Long)
    suspend fun addTracksToPlaylist(playlistId: Long, trackIds: List<Long>)
    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: Long)
    suspend fun reorderPlaylist(playlistId: Long, orderedTrackIds: List<Long>)
}

