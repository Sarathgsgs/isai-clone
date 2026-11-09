package com.isai.player.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.isai.player.data.model.Track
import com.isai.player.data.model.TrackSortOrder
import com.isai.player.data.repository.HistoryRepository
import com.isai.player.data.repository.LibraryRepository
import com.isai.player.playback.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val historyRepository: HistoryRepository,
    private val libraryRepository: LibraryRepository,
    private val playerController: PlayerController
) : ViewModel() {

    val recentlyPlayed: StateFlow<List<Track>> = historyRepository.observeRecentlyPlayed(limit = 20)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalSongsCount: StateFlow<Int> = libraryRepository.observeTrackCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val albumsCount: StateFlow<Int> = libraryRepository.observeAlbums()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val artistsCount: StateFlow<Int> = libraryRepository.observeArtists()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun playTrack(track: Track, contextQueue: List<Track> = emptyList()) {
        val queue = if (contextQueue.isNotEmpty()) contextQueue else listOf(track)
        val index = queue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        playerController.playTrack(track, queue, index)
    }

    fun shuffleAll() {
        viewModelScope.launch {
            val allTracks = libraryRepository.observeTracks(TrackSortOrder.TITLE_ASC).first()
            if (allTracks.isNotEmpty()) {
                val shuffled = allTracks.shuffled()
                playerController.setShuffleModeEnabled(true)
                playerController.playTrack(shuffled.first(), shuffled, 0)
            }
        }
    }
}

