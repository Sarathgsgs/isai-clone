package com.isai.player.ui.widget

import android.content.Context
import com.isai.player.playback.PlayerController
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WidgetUpdaterTest {

    private val context = mockk<Context>(relaxed = true)
    private val playerController = mockk<PlayerController>(relaxed = true)
    private lateinit var widgetUpdater: WidgetUpdater

    @Before
    fun setUp() {
        widgetUpdater = WidgetUpdater(context, playerController)
    }

    @Test
    fun shouldTriggerUpdate_whenTrackIdChanges_returnsTrueImmediately() {
        val shouldUpdate = widgetUpdater.shouldTriggerUpdate(
            now = 1000L,
            trackId = 200L,
            isPlaying = true,
            lastTrackId = 100L,
            lastIsPlaying = true,
            lastUpdateTime = 999L
        )
        assertTrue(shouldUpdate)
    }

    @Test
    fun shouldTriggerUpdate_whenPlayStateChanges_returnsTrueImmediately() {
        val shouldUpdate = widgetUpdater.shouldTriggerUpdate(
            now = 1000L,
            trackId = 100L,
            isPlaying = false,
            lastTrackId = 100L,
            lastIsPlaying = true,
            lastUpdateTime = 999L
        )
        assertTrue(shouldUpdate)
    }

    @Test
    fun shouldTriggerUpdate_whenElapsedMoreThanTenSeconds_returnsTrue() {
        val shouldUpdate = widgetUpdater.shouldTriggerUpdate(
            now = 20_000L,
            trackId = 100L,
            isPlaying = true,
            lastTrackId = 100L,
            lastIsPlaying = true,
            lastUpdateTime = 10_000L
        )
        assertTrue(shouldUpdate)
    }

    @Test
    fun shouldTriggerUpdate_whenElapsedLessThanTenSecondsAndSameState_returnsFalse() {
        val shouldUpdate = widgetUpdater.shouldTriggerUpdate(
            now = 15_000L,
            trackId = 100L,
            isPlaying = true,
            lastTrackId = 100L,
            lastIsPlaying = true,
            lastUpdateTime = 10_000L
        )
        assertFalse(shouldUpdate)
    }
}

