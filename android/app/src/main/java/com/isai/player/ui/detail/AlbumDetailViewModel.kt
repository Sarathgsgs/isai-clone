package com.isai.player.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.isai.player.data.model.Track
import com.isai.player.data.repository.FavoritesRepository
import com.isai.player.data.repository.LibraryRepository
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

data class AlbumDetailUiState(
    val albumTitle: String = "",
    val albumArtist: String = "",
    val artPath: String? = null,
    val releaseYear: Int = 0,
    val tracks: List<Track> = emptyList(),
    val discGroups: Map<Int, List<Track>> = emptyMap(),
    val totalDurationMs: Long = 0L,
    val favoriteTrackIds: Set<Long> = emptySet(),
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AlbumDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val libraryRepository: LibraryRepository,
    private val playerController: PlayerController,
    private val favoritesRepository: FavoritesRepository
) : ViewModel() {

    private val albumKeyFlow = MutableStateFlow(savedStateHandle.get<String>("albumKey") ?: "")

    val uiState: StateFlow<AlbumDetailUiState> = albumKeyFlow
        .flatMapLatest { albumKey ->
            combine(
                libraryRepository.observeTracksForAlbum(albumKey),
                favoritesRepository.observeFavoriteTrackIds()
            ) { tracks, favIds ->
                val primaryTrack = tracks.firstOrNull()
                val totalDuration = tracks.sumOf { it.durationMs }
                val groups = tracks.groupBy { it.discNo.coerceAtLeast(1) }.toSortedMap()

                AlbumDetailUiState(
                    albumTitle = albumKey,
                    albumArtist = primaryTrack?.albumArtist?.ifBlank { primaryTrack.artist } ?: "",
                    artPath = tracks.firstOrNull { it.artPath != null }?.artPath,
                    releaseYear = tracks.map { it.year }.filter { it > 0 }.minOrNull() ?: 0,
                    tracks = tracks,
                    discGroups = groups,
                    totalDurationMs = totalDuration,
                    favoriteTrackIds = favIds,
                    isLoading = false
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AlbumDetailUiState(albumTitle = albumKeyFlow.value)
        )

    fun setAlbumKey(key: String) {
        albumKeyFlow.value = key
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
}

