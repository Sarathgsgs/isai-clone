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

data class ArtistAlbumSummary(
    val title: String,
    val artPath: String?,
    val year: Int,
    val trackCount: Int
)

data class ArtistDetailUiState(
    val artistName: String = "",
    val sampleArtPath: String? = null,
    val albums: List<ArtistAlbumSummary> = emptyList(),
    val tracks: List<Track> = emptyList(),
    val favoriteTrackIds: Set<Long> = emptySet(),
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ArtistDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val libraryRepository: LibraryRepository,
    private val playerController: PlayerController,
    private val favoritesRepository: FavoritesRepository
) : ViewModel() {

    private val artistKeyFlow = MutableStateFlow(savedStateHandle.get<String>("artistKey") ?: "")

    val uiState: StateFlow<ArtistDetailUiState> = artistKeyFlow
        .flatMapLatest { artistKey ->
            combine(
                libraryRepository.observeTracksForArtist(artistKey),
                favoritesRepository.observeFavoriteTrackIds()
            ) { tracks, favIds ->
                val sampleArt = tracks.firstOrNull { it.artPath != null }?.artPath

                // Group tracks into distinct albums for the discography carousel
                val albumSummaries = tracks
                    .filter { it.album.isNotBlank() }
                    .groupBy { it.album }
                    .map { (albumTitle, albumTracks) ->
                        ArtistAlbumSummary(
                            title = albumTitle,
                            artPath = albumTracks.firstOrNull { it.artPath != null }?.artPath,
                            year = albumTracks.map { it.year }.filter { it > 0 }.minOrNull() ?: 0,
                            trackCount = albumTracks.size
                        )
                    }
                    .sortedByDescending { it.year }

                ArtistDetailUiState(
                    artistName = artistKey,
                    sampleArtPath = sampleArt,
                    albums = albumSummaries,
                    tracks = tracks,
                    favoriteTrackIds = favIds,
                    isLoading = false
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ArtistDetailUiState(artistName = artistKeyFlow.value)
        )

    fun setArtistKey(key: String) {
        artistKeyFlow.value = key
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

