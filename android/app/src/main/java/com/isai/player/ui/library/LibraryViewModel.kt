package com.isai.player.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.isai.player.data.model.Album
import com.isai.player.data.model.Artist
import com.isai.player.data.model.Folder
import com.isai.player.data.model.Playlist
import com.isai.player.data.model.Track
import com.isai.player.data.model.TrackSortOrder
import com.isai.player.data.repository.FavoritesRepository
import com.isai.player.data.repository.LibraryRepository
import com.isai.player.data.repository.PlaylistRepository
import com.isai.player.playback.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val favoritesRepository: FavoritesRepository,
    private val playlistRepository: PlaylistRepository,
    private val playerController: PlayerController
) : ViewModel() {

    private val _selectedTabIndex = MutableStateFlow(0)
    val selectedTabIndex: StateFlow<Int> = _selectedTabIndex.asStateFlow()

    private val _sortOrder = MutableStateFlow(TrackSortOrder.TITLE_ASC)
    val sortOrder: StateFlow<TrackSortOrder> = _sortOrder.asStateFlow()

    val tracks: StateFlow<List<Track>> = _sortOrder
        .flatMapLatest { order ->
            libraryRepository.observeTracks(order)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val albums: StateFlow<List<Album>> = libraryRepository.observeAlbums()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val artists: StateFlow<List<Artist>> = libraryRepository.observeArtists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlists: StateFlow<List<Playlist>> = playlistRepository.observePlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val folders: StateFlow<List<Folder>> = libraryRepository.observeFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteTrackIds: StateFlow<Set<Long>> = favoritesRepository.observeFavoriteTrackIds()
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    fun selectTab(index: Int) {
        _selectedTabIndex.value = index
    }

    fun setSortOrder(order: TrackSortOrder) {
        _sortOrder.value = order
    }

    fun playTrack(track: Track, contextList: List<Track> = emptyList()) {
        val queue = if (contextList.isNotEmpty()) contextList else listOf(track)
        val index = queue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        playerController.playTrack(track, queue, index)
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

    fun createPlaylist(name: String, initialTrackIds: List<Long> = emptyList()) {
        viewModelScope.launch {
            val playlistId = playlistRepository.createPlaylist(name)
            if (initialTrackIds.isNotEmpty()) {
                playlistRepository.addTracksToPlaylist(playlistId, initialTrackIds)
            }
        }
    }

    fun addTracksToPlaylist(playlistId: Long, trackIds: List<Long>) {
        viewModelScope.launch {
            playlistRepository.addTracksToPlaylist(playlistId, trackIds)
        }
    }
}

