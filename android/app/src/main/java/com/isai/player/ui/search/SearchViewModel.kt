package com.isai.player.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.isai.player.data.model.Album
import com.isai.player.data.model.Artist
import com.isai.player.data.model.Track
import com.isai.player.data.repository.FavoritesRepository
import com.isai.player.data.repository.LibraryRepository
import com.isai.player.playback.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val isSearching: Boolean = false,
    val matchingTracks: List<Track> = emptyList(),
    val matchingAlbums: List<Album> = emptyList(),
    val matchingArtists: List<Artist> = emptyList(),
    val favoriteTrackIds: Set<Long> = emptySet()
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val playerController: PlayerController,
    private val favoritesRepository: FavoritesRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val uiState: StateFlow<SearchUiState> = _searchQuery
        .debounce(300L)
        .distinctUntilChanged()
        .flatMapLatest { query ->
            val trimmed = query.trim()
            if (trimmed.isEmpty()) {
                flowOf(SearchUiState(query = query))
            } else {
                combine(
                    libraryRepository.searchTracks(trimmed),
                    favoritesRepository.observeFavoriteTrackIds()
                ) { tracks, favIds ->
                    // Categorize results
                    val albums = tracks
                        .filter { it.album.isNotBlank() }
                        .groupBy { it.album }
                        .map { (albumTitle, albumTracks) ->
                            Album(
                                title = albumTitle,
                                artist = albumTracks.firstOrNull()?.albumArtist ?: albumTracks.firstOrNull()?.artist ?: "",
                                artPath = albumTracks.firstOrNull { it.artPath != null }?.artPath,
                                trackCount = albumTracks.size,
                                year = albumTracks.map { it.year }.filter { it > 0 }.minOrNull() ?: 0
                            )
                        }

                    val artists = tracks
                        .filter { it.artist.isNotBlank() }
                        .groupBy { it.artist }
                        .map { (artistName, artistTracks) ->
                            Artist(
                                name = artistName,
                                trackCount = artistTracks.size,
                                albumCount = artistTracks.map { it.album }.filter { it.isNotBlank() }.distinct().size,
                                sampleArtPath = artistTracks.firstOrNull { it.artPath != null }?.artPath
                            )
                        }

                    SearchUiState(
                        query = query,
                        isSearching = false,
                        matchingTracks = tracks,
                        matchingAlbums = albums,
                        matchingArtists = artists,
                        favoriteTrackIds = favIds
                    )
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SearchUiState()
        )

    fun onQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun clearQuery() {
        _searchQuery.value = ""
    }

    fun playTrack(track: Track) {
        val tracks = uiState.value.matchingTracks
        val index = tracks.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        playerController.playTrack(track, tracks, index)
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

