package com.isai.player.ui.playlists

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.isai.player.data.model.Playlist
import com.isai.player.data.model.Track
import com.isai.player.data.repository.FavoritesRepository
import com.isai.player.data.repository.LibraryRepository
import com.isai.player.data.repository.PlaylistRepository
import com.isai.player.playback.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlaylistDetailUiState(
    val playlist: Playlist? = null,
    val tracks: List<Track> = emptyList(),
    val allLibraryTracks: List<Track> = emptyList(),
    val totalDurationMs: Long = 0L,
    val favoriteTrackIds: Set<Long> = emptySet(),
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val playlistRepository: PlaylistRepository,
    private val libraryRepository: LibraryRepository,
    private val favoritesRepository: FavoritesRepository,
    private val playerController: PlayerController
) : ViewModel() {

    private val playlistIdFlow = MutableStateFlow(savedStateHandle.get<Long>("playlistId") ?: 0L)

    val uiState: StateFlow<PlaylistDetailUiState> = playlistIdFlow
        .flatMapLatest { playlistId ->
            combine(
                playlistRepository.observePlaylistById(playlistId),
                playlistRepository.observeTracksForPlaylist(playlistId),
                libraryRepository.observeTracks(),
                favoritesRepository.observeFavoriteTrackIds()
            ) { playlist, tracks, allTracks, favIds ->
                PlaylistDetailUiState(
                    playlist = playlist,
                    tracks = tracks,
                    allLibraryTracks = allTracks,
                    totalDurationMs = tracks.sumOf { it.durationMs },
                    favoriteTrackIds = favIds.toSet(),
                    isLoading = false
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PlaylistDetailUiState()
        )

    fun setPlaylistId(id: Long) {
        playlistIdFlow.value = id
    }

    fun playTrack(track: Track) {
        val tracks = uiState.value.tracks
        val index = tracks.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        playerController.playTrack(track, tracks, index)
    }

    fun playAll() {
        val tracks = uiState.value.tracks
        if (tracks.isNotEmpty()) {
            playerController.playTracks(tracks, 0)
        }
    }

    fun shuffleAll() {
        val tracks = uiState.value.tracks
        if (tracks.isNotEmpty()) {
            playerController.playTracks(tracks.shuffled(), 0)
        }
    }

    fun playNext(track: Track) {
        playerController.playNext(track)
    }

    fun addToQueue(track: Track) {
        playerController.addToQueue(listOf(track))
    }

    fun toggleFavorite(trackId: Long) {
        viewModelScope.launch {
            favoritesRepository.toggleFavorite(trackId)
        }
    }

    fun renamePlaylist(newName: String) {
        val id = playlistIdFlow.value
        if (id > 0 && newName.isNotBlank()) {
            viewModelScope.launch {
                playlistRepository.renamePlaylist(id, newName.trim())
            }
        }
    }

    fun deletePlaylist(onDeleted: () -> Unit) {
        val id = playlistIdFlow.value
        if (id > 0) {
            viewModelScope.launch {
                playlistRepository.deletePlaylist(id)
                onDeleted()
            }
        }
    }

    fun addTracksToPlaylist(trackIds: List<Long>) {
        val id = playlistIdFlow.value
        if (id > 0 && trackIds.isNotEmpty()) {
            viewModelScope.launch {
                playlistRepository.addTracksToPlaylist(id, trackIds)
            }
        }
    }

    fun removeTrackFromPlaylist(trackId: Long) {
        val id = playlistIdFlow.value
        if (id > 0) {
            viewModelScope.launch {
                playlistRepository.removeTrackFromPlaylist(id, trackId)
            }
        }
    }
}

