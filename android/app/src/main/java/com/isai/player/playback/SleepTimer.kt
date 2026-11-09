package com.isai.player.playback

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SleepTimer @Inject constructor() {

    private val _remainingMs = MutableStateFlow(0L)
    val remainingMs: StateFlow<Long> = _remainingMs.asStateFlow()

    private val _isStopAtEndOfTrack = MutableStateFlow(false)
    val isStopAtEndOfTrack: StateFlow<Boolean> = _isStopAtEndOfTrack.asStateFlow()

    private var timerJob: Job? = null

    val isRunning: Boolean
        get() = _remainingMs.value > 0L || _isStopAtEndOfTrack.value

    fun startTimer(
        durationMs: Long,
        stopAtEndOfTrack: Boolean,
        scope: CoroutineScope,
        onVolumeUpdate: (Float) -> Unit,
        onTimerComplete: () -> Unit
    ) {
        cancel(onVolumeUpdate)
        _isStopAtEndOfTrack.value = stopAtEndOfTrack

        if (stopAtEndOfTrack) {
            _remainingMs.value = 0L
            return
        }

        _remainingMs.value = durationMs
        val targetEpochMs = System.currentTimeMillis() + durationMs

        timerJob = scope.launch {
            while (isActive) {
                delay(250L)
                val remaining = targetEpochMs - System.currentTimeMillis()
                if (remaining <= 0L) {
                    _remainingMs.value = 0L
                    onVolumeUpdate(1f)
                    onTimerComplete()
                    break
                }

                _remainingMs.value = remaining

                // 5-second fade out
                if (remaining in 1..5000L) {
                    val fadeFactor = (remaining.toFloat() / 5000f).coerceIn(0f, 1f)
                    onVolumeUpdate(fadeFactor)
                } else {
                    onVolumeUpdate(1f)
                }
            }
        }
    }

    fun onTrackEnded(onTimerComplete: () -> Unit) {
        if (_isStopAtEndOfTrack.value) {
            cancel()
            onTimerComplete()
        }
    }

    fun cancel(onVolumeUpdate: ((Float) -> Unit)? = null) {
        timerJob?.cancel()
        timerJob = null
        _remainingMs.value = 0L
        _isStopAtEndOfTrack.value = false
        onVolumeUpdate?.invoke(1f)
    }
}

