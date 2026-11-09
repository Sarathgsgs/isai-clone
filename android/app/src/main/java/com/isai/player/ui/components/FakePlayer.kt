package com.isai.player.ui.components

import com.isai.player.data.repository.LiftBehavior
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Lightweight fake player simulating realistic audio playback and timeline state
 * for isolated component labs (Prompts A, B, C, D).
 */
class FakePlayer(
    private val scope: CoroutineScope,
    val durationMs: Long = 217_000L // 3 minutes 37 seconds
) {
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _hasTrack = MutableStateFlow(true)
    val hasTrack: StateFlow<Boolean> = _hasTrack.asStateFlow()

    var liftBehavior: LiftBehavior = LiftBehavior.PAUSE

    private var playbackJob: Job? = null

    init {
        startTicker()
    }

    private fun startTicker() {
        playbackJob?.cancel()
        playbackJob = scope.launch {
            var lastTime = System.currentTimeMillis()
            while (isActive) {
                val now = System.currentTimeMillis()
                val delta = now - lastTime
                lastTime = now

                if (_isPlaying.value && _hasTrack.value) {
                    val nextPos = _positionMs.value + delta
                    if (nextPos >= durationMs) {
                        _positionMs.value = durationMs
                        _isPlaying.value = false
                    } else {
                        _positionMs.value = nextPos
                    }
                }
                delay(16L) // ~60 fps ticker
            }
        }
    }

    fun play() {
        if (!_hasTrack.value) return
        if (_positionMs.value >= durationMs) {
            _positionMs.value = 0L
        }
        _isPlaying.value = true
    }

    fun pause() {
        _isPlaying.value = false
    }

    fun togglePlayPause() {
        if (_isPlaying.value) pause() else play()
    }

    fun seekTo(positionMs: Long) {
        _positionMs.value = positionMs.coerceIn(0L, durationMs)
    }

    fun seekToProgress(progress: Float) {
        val targetMs = (progress.coerceIn(0f, 1f) * durationMs).toLong()
        seekTo(targetMs)
    }

    fun handleLiftedStop() {
        pause()
        if (liftBehavior == LiftBehavior.STOP) {
            seekTo(0L)
        }
    }

    fun setHasTrack(hasTrack: Boolean) {
        _hasTrack.value = hasTrack
        if (!hasTrack) {
            pause()
        }
    }
}

