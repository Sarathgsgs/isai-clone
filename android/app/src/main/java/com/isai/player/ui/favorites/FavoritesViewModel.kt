package com.isai.player.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.isai.player.data.model.Track
import com.isai.player.data.repository.FavoritesRepository
import com.isai.player.playback.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FavoritesUiState(
    val tracks: List<Track> = emptyList(),
    val totalDurationMs: Long = 0L,
    val isLoading: Boolean = true
)

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val favoritesRepository: FavoritesRepository,
    private val playerController: PlayerController
) : ViewModel() {

    val uiState: StateFlow<FavoritesUiState> = favoritesRepository.observeFavoriteTracks()
        .map { tracks ->
            FavoritesUiState(
                tracks = tracks,
                totalDurationMs = tracks.sumOf { it.durationMs },
                isLoading = false
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = FavoritesUiState()
        )

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

