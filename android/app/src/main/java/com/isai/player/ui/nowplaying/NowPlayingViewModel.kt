package com.isai.player.ui.nowplaying

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.isai.player.data.lyrics.LrcParser
import com.isai.player.data.lyrics.LyricsFormat
import com.isai.player.data.model.Track
import com.isai.player.data.repository.FavoritesRepository
import com.isai.player.data.repository.LiftBehavior
import com.isai.player.data.repository.LyricsRepository
import com.isai.player.data.repository.LyricsState
import com.isai.player.data.repository.LyricsTextSize
import com.isai.player.data.repository.SeekBarStyle
import com.isai.player.data.repository.SettingsRepository
import com.isai.player.data.repository.WaveformRepository
import com.isai.player.data.repository.WaveformState
import com.isai.player.playback.PlayerController
import com.isai.player.playback.PlayerState
import com.isai.player.playback.RepeatMode
import com.isai.player.ui.components.StageTrack
import com.isai.player.ui.components.SwipeDirection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ScrubOwner {
    NONE,
    ARM,
    BAR
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class NowPlayingViewModel @Inject constructor(
    private val playerController: PlayerController,
    private val favoritesRepository: FavoritesRepository,
    private val settingsRepository: SettingsRepository,
    private val waveformRepository: WaveformRepository,
    private val lyricsRepository: LyricsRepository
) : ViewModel() {

    val playerState: StateFlow<PlayerState> = playerController.playerState

    val stageTrack: StateFlow<StageTrack?> = playerState.map { state ->
        state.currentTrack?.toStageTrack()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isFavorite: StateFlow<Boolean> = playerState
        .flatMapLatest { state ->
            val trackId = state.currentTrack?.id
            if (trackId != null) {
                favoritesRepository.observeIsFavorite(trackId)
            } else {
                flowOf(false)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val liftBehavior: StateFlow<LiftBehavior> = settingsRepository.liftBehavior
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LiftBehavior.PAUSE)

    val seekBarStyle: StateFlow<SeekBarStyle> = settingsRepository.seekBarStyle
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SeekBarStyle.AUTO)

    val lyricsTextSize: StateFlow<LyricsTextSize> = settingsRepository.lyricsTextSize
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LyricsTextSize.MEDIUM)

    val keepScreenOnWhileLyrics: StateFlow<Boolean> = settingsRepository.keepScreenOnWhileLyrics
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val sleepTimerRemainingMs: StateFlow<Long> = playerController.sleepTimer.remainingMs
    val isStopAtEndOfTrack: StateFlow<Boolean> = playerController.sleepTimer.isStopAtEndOfTrack

    val waveform: StateFlow<WaveformState> = playerState
        .flatMapLatest { state ->
            val track = state.currentTrack
            if (track != null && track.durationMs > 0) {
                waveformRepository.observeWaveform(
                    trackId = track.id,
                    durationMs = track.durationMs,
                    trackUri = Uri.parse(track.uri),
                    lastModified = track.lastModified
                )
            } else {
                flowOf(WaveformState.Unavailable)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WaveformState.Loading)

    val isLyricsVisible = MutableStateFlow(false)

    val lyricsState: StateFlow<LyricsState> = playerState
        .flatMapLatest { state ->
            val track = state.currentTrack
            if (track != null) {
                lyricsRepository.observeLyrics(track).map { parsed ->
                    if (parsed == null || parsed.lines.isEmpty()) {
                        LyricsState.Empty
                    } else if (parsed.format == LyricsFormat.SYNCED) {
                        val effectiveTime = state.positionMs + parsed.offsetMs
                        val activeIndex = LrcParser.lineIndexAt(parsed.lines, effectiveTime)
                        LyricsState.Synced(parsed.lines, activeIndex, parsed.offsetMs)
                    } else {
                        LyricsState.Plain(parsed.lines.map { it.text })
                    }
                }
            } else {
                flowOf(LyricsState.Empty)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LyricsState.Loading)

    private val _scrubOwner = MutableStateFlow(ScrubOwner.NONE)
    val scrubOwner: StateFlow<ScrubOwner> = _scrubOwner.asStateFlow()

    private val _scrubPreviewProgress = MutableStateFlow<Float?>(null)
    val scrubPreviewProgress: StateFlow<Float?> = _scrubPreviewProgress.asStateFlow()

    private val _lastSwipeDirection = MutableStateFlow(SwipeDirection.NONE)
    val lastSwipeDirection: StateFlow<SwipeDirection> = _lastSwipeDirection.asStateFlow()

    init {
        // Prefetch waveform for next track in queue when playing
        playerState.onEach { state ->
            if (state.isPlaying && state.queue.isNotEmpty()) {
                val nextIndex = state.queueIndex + 1
                if (nextIndex < state.queue.size) {
                    val nextTrack = state.queue[nextIndex]
                    waveformRepository.prefetchWaveform(
                        trackId = nextTrack.id,
                        durationMs = nextTrack.durationMs,
                        trackUri = Uri.parse(nextTrack.uri),
                        lastModified = nextTrack.lastModified
                    )
                }
            }
        }.launchIn(viewModelScope)
    }

    fun togglePlayPause() {
        playerController.togglePlayPause()
    }

    fun toggleLyricsView() {
        isLyricsVisible.value = !isLyricsVisible.value
    }

    fun setLyricsOffset(offsetMs: Int) {
        val currentId = playerState.value.currentTrack?.id ?: return
        viewModelScope.launch {
            lyricsRepository.updateLyricsOffset(currentId, offsetMs)
        }
    }

    fun importLyrics(rawText: String) {
        val currentId = playerState.value.currentTrack?.id ?: return
        viewModelScope.launch {
            lyricsRepository.importLyrics(currentId, rawText)
        }
    }

    fun seekToLyricTimestamp(timestampMs: Long) {
        playerController.seekTo(timestampMs)
        playerController.play()
    }

    fun onScrubStart(owner: ScrubOwner = ScrubOwner.NONE) {
        if (_scrubOwner.value == ScrubOwner.NONE || _scrubOwner.value == owner) {
            _scrubOwner.value = owner
        }
    }

    fun onScrubPreview(owner: ScrubOwner = ScrubOwner.NONE, progress: Float) {
        if (_scrubOwner.value == ScrubOwner.NONE || _scrubOwner.value == owner) {
            _scrubOwner.value = owner
            _scrubPreviewProgress.value = progress
        }
    }

    fun onScrubEnd(owner: ScrubOwner = ScrubOwner.NONE, commit: Boolean, progress: Float) {
        if (_scrubOwner.value == ScrubOwner.NONE || _scrubOwner.value == owner) {
            _scrubOwner.value = ScrubOwner.NONE
            _scrubPreviewProgress.value = null
            if (commit) {
                playerController.seekToProgress(progress)
                playerController.play()
            }
        }
    }

    fun onLiftedStop() {
        _scrubOwner.value = ScrubOwner.NONE
        _scrubPreviewProgress.value = null
        val behavior = liftBehavior.value
        if (behavior == LiftBehavior.STOP) {
            playerController.pause()
            playerController.seekTo(0L)
        } else {
            playerController.pause()
        }
    }

    fun skipToNext() {
        _lastSwipeDirection.value = SwipeDirection.NEXT
        playerController.skipToNext()
    }

    fun skipToPrevious() {
        _lastSwipeDirection.value = SwipeDirection.PREVIOUS
        playerController.skipToPrevious()
    }

    fun toggleFavorite() {
        val currentId = playerState.value.currentTrack?.id ?: return
        viewModelScope.launch {
            favoritesRepository.toggleFavorite(currentId)
        }
    }

    fun toggleShuffle() {
        playerController.toggleShuffle()
    }

    fun cycleRepeatMode() {
        val nextMode = when (playerState.value.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        playerController.setRepeatMode(nextMode)
    }

    fun clearSwipeDirection() {
        _lastSwipeDirection.value = SwipeDirection.NONE
    }

    fun removeQueueItem(index: Int) {
        playerController.removeFromQueue(index)
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        playerController.moveQueueItem(fromIndex, toIndex)
    }

    fun playQueueIndex(index: Int) {
        playerController.playTrackAtIndex(index)
    }
}
