package com.isai.player.playback

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.isai.player.data.equalizer.EffectCapabilities
import com.isai.player.data.model.Track
import com.isai.player.playback.audio.AudioEffectsManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val queueStore: QueueStore,
    private val playHistoryTracker: PlayHistoryTracker,
    val sleepTimer: SleepTimer,
    val audioEffectsManager: AudioEffectsManager
) {

    private val controllerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var mediaController: MediaController? = null
    private var positionTickerJob: Job? = null

    val effectCapabilities: StateFlow<EffectCapabilities> = audioEffectsManager.capabilities

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    fun launchSystemEqualizer(ctx: Context): Boolean {
        val intent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
            putExtra(AudioEffect.EXTRA_AUDIO_SESSION, audioEffectsManager.currentSessionId)
            putExtra(AudioEffect.EXTRA_PACKAGE_NAME, ctx.packageName)
            putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return try {
            if (intent.resolveActivity(ctx.packageManager) != null) {
                ctx.startActivity(intent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    init {
        connectToService()
    }

    private fun connectToService() {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()

        controllerFuture.addListener(
            {
                try {
                    val controller = controllerFuture.get()
                    mediaController = controller
                    setupPlayerListener(controller)
                    syncPlayerState(controller)
                } catch (e: Exception) {
                    // Service connection failed or deferred
                }
            },
            ContextCompat.getMainExecutor(context)
        )
    }

    private fun setupPlayerListener(player: Player) {
        player.addListener(object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                syncPlayerState(player)
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    _playerState.update { it.copy(playbackErrorMessage = null) }
                    startPositionTicker()
                } else {
                    stopPositionTicker()
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                _playerState.update {
                    it.copy(playbackErrorMessage = "Playback error: ${error.errorCodeName}")
                }
            }
        })
    }

    private fun syncPlayerState(player: Player) {
        val currentMediaItem = player.currentMediaItem
        val currentTrack = currentMediaItem?.let(MediaItemMapper::toTrack)
        val queue = (0 until player.mediaItemCount).mapNotNull { index ->
            MediaItemMapper.toTrack(player.getMediaItemAt(index))
        }

        val repeatMode = when (player.repeatMode) {
            Player.REPEAT_MODE_ONE -> RepeatMode.ONE
            Player.REPEAT_MODE_ALL -> RepeatMode.ALL
            else -> RepeatMode.OFF
        }

        _playerState.update {
            it.copy(
                currentTrack = currentTrack,
                isPlaying = player.isPlaying,
                playbackState = player.playbackState,
                currentPositionMs = player.currentPosition.coerceAtLeast(0L),
                durationMs = player.duration.coerceAtLeast(0L),
                bufferedPositionMs = player.bufferedPosition.coerceAtLeast(0L),
                shuffleModeEnabled = player.shuffleModeEnabled,
                repeatMode = repeatMode,
                queue = queue,
                currentQueueIndex = player.currentMediaItemIndex,
                isConnected = true
            )
        }

        if (player.isPlaying) {
            startPositionTicker()
        } else {
            stopPositionTicker()
        }
    }

    private fun startPositionTicker() {
        if (positionTickerJob?.isActive == true) return

        positionTickerJob = controllerScope.launch {
            while (isActive) {
                val player = mediaController
                if (player != null && player.isPlaying) {
                    val currentPos = player.currentPosition.coerceAtLeast(0L)
                    _playerState.update { it.copy(currentPositionMs = currentPos) }
                    playHistoryTracker.onPositionUpdate(currentPos, isPlaying = true, scope = controllerScope)
                } else {
                    break
                }
                delay(200L)
            }
        }
    }

    private fun stopPositionTicker() {
        positionTickerJob?.cancel()
        positionTickerJob = null
    }

    // Playback Commands
    fun play() {
        mediaController?.play()
    }

    fun pause() {
        mediaController?.pause()
    }

    fun togglePlayPause() {
        val player = mediaController ?: return
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    }

    fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs)
        _playerState.update { it.copy(currentPositionMs = positionMs) }
    }

    fun seekToProgress(progress: Float) {
        val duration = _playerState.value.durationMs
        if (duration > 0L) {
            val targetMs = (progress * duration).toLong().coerceIn(0L, duration)
            seekTo(targetMs)
        }
    }

    fun skipToNext() {
        val player = mediaController ?: return
        if (player.hasNextMediaItem()) {
            player.seekToNextMediaItem()
        } else if (_playerState.value.queue.isNotEmpty()) {
            // Loop back to start if at the end of queue
            player.seekToDefaultPosition(0)
        }
    }

    /**
     * Skips to the previous track, honoring the 3-second rule:
     * - If elapsed position > 3000ms: restarts the current track at 0:00.
     * - If elapsed position <= 3000ms: moves to the previous track in the queue.
     */
    fun skipToPrevious() {
        val player = mediaController ?: return
        if (player.currentPosition > 3000L) {
            player.seekTo(0L)
            _playerState.update { it.copy(currentPositionMs = 0L) }
        } else {
            if (player.hasPreviousMediaItem()) {
                player.seekToPreviousMediaItem()
            } else if (_playerState.value.queue.isNotEmpty()) {
                player.seekToDefaultPosition(_playerState.value.queue.lastIndex)
            }
        }
    }

    fun setShuffleModeEnabled(enabled: Boolean) {
        mediaController?.shuffleModeEnabled = enabled
    }

    fun cycleRepeatMode() {
        val player = mediaController ?: return
        val nextMode = _playerState.value.repeatMode.next()
        val exoMode = when (nextMode) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
        }
        player.repeatMode = exoMode
    }

    // Queue Management
    fun playTracks(tracks: List<Track>, startIndex: Int = 0) {
        if (tracks.isEmpty()) return
        val validIndex = startIndex.coerceIn(0, tracks.lastIndex)
        playTrack(tracks[validIndex], tracks, validIndex)
    }

    fun playTrack(track: Track, queue: List<Track>, startIndex: Int = queue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)) {
        val player = mediaController ?: return
        val mediaItems = queue.map(MediaItemMapper::toMediaItem)
        player.setMediaItems(mediaItems, startIndex, 0L)
        player.prepare()
        player.play()

        controllerScope.launch {
            queueStore.saveQueue(queue)
        }
    }

    fun playNext(track: Track) {
        val player = mediaController ?: return
        val insertIndex = (player.currentMediaItemIndex + 1).coerceAtMost(player.mediaItemCount)
        player.addMediaItem(insertIndex, MediaItemMapper.toMediaItem(track))

        controllerScope.launch {
            val updatedQueue = _playerState.value.queue.toMutableList().apply {
                add(insertIndex.coerceAtMost(size), track)
            }
            queueStore.saveQueue(updatedQueue)
        }
    }

    fun addToQueue(tracks: List<Track>) {
        val player = mediaController ?: return
        val mediaItems = tracks.map(MediaItemMapper::toMediaItem)
        player.addMediaItems(mediaItems)

        controllerScope.launch {
            val updatedQueue = _playerState.value.queue + tracks
            queueStore.saveQueue(updatedQueue)
        }
    }

    fun removeFromQueue(index: Int) {
        val player = mediaController ?: return
        if (index in 0 until player.mediaItemCount) {
            player.removeMediaItem(index)
            controllerScope.launch {
                val updatedQueue = _playerState.value.queue.toMutableList().apply {
                    if (index in 0 until size) removeAt(index)
                }
                queueStore.saveQueue(updatedQueue)
            }
        }
    }

    fun reorderQueue(fromIndex: Int, toIndex: Int) {
        val player = mediaController ?: return
        if (fromIndex in 0 until player.mediaItemCount && toIndex in 0 until player.mediaItemCount) {
            player.moveMediaItem(fromIndex, toIndex)
            controllerScope.launch {
                val updatedQueue = _playerState.value.queue.toMutableList().apply {
                    val item = removeAt(fromIndex)
                    add(toIndex, item)
                }
                queueStore.saveQueue(updatedQueue)
            }
        }
    }

    fun clearQueue() {
        val player = mediaController ?: return
        player.clearMediaItems()
        controllerScope.launch {
            queueStore.clearQueue()
        }
    }

    fun setVolume(volume: Float) {
        mediaController?.volume = volume.coerceIn(0f, 1f)
    }

    fun startSleepTimer(durationMs: Long, stopAtEndOfTrack: Boolean) {
        sleepTimer.startTimer(
            durationMs = durationMs,
            stopAtEndOfTrack = stopAtEndOfTrack,
            scope = controllerScope,
            onVolumeUpdate = { setVolume(it) },
            onTimerComplete = { pause() }
        )
    }

    fun cancelSleepTimer() {
        sleepTimer.cancel { setVolume(1f) }
    }
}
