package com.isai.player.ui.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.isai.player.playback.PlayerController
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Observes PlayerState and triggers throttled updates for IsaiGlanceWidget.
 *
 * Updates immediately on track changes or play/pause toggles.
 * Throttles position progress updates to at most once every 10 seconds to avoid IPC saturation.
 */
@Singleton
class WidgetUpdater @Inject constructor(
    @ApplicationContext private val context: Context,
    private val playerController: PlayerController
) {
    private val updaterScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var observerJob: Job? = null

    private var lastTrackId: Long? = null
    private var lastIsPlaying: Boolean? = null
    private var lastUpdateTimeMs: Long = 0L

    fun start() {
        if (observerJob != null) return

        observerJob = updaterScope.launch {
            playerController.playerState.collectLatest { state ->
                val now = System.currentTimeMillis()
                val currentTrackId = state.currentTrack?.id
                val currentIsPlaying = state.isPlaying

                val isTrackChanged = currentTrackId != lastTrackId
                val isPlayStateChanged = currentIsPlaying != lastIsPlaying
                val isPeriodicUpdate = (now - lastUpdateTimeMs) >= 10_000L

                if (isTrackChanged || isPlayStateChanged || isPeriodicUpdate) {
                    lastTrackId = currentTrackId
                    lastIsPlaying = currentIsPlaying
                    lastUpdateTimeMs = now

                    runCatching {
                        IsaiGlanceWidget().updateAll(context)
                    }
                }
            }
        }
    }

    fun stop() {
        observerJob?.cancel()
        observerJob = null
    }

    /**
     * Diagnostic helper for testing throttle logic.
     */
    fun shouldTriggerUpdate(
        now: Long,
        trackId: Long?,
        isPlaying: Boolean,
        lastTrackId: Long?,
        lastIsPlaying: Boolean?,
        lastUpdateTime: Long
    ): Boolean {
        if (trackId != lastTrackId) return true
        if (isPlaying != lastIsPlaying) return true
        return (now - lastUpdateTime) >= 10_000L
    }
}

