package com.isai.player.ui.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class WidgetArtRendererTest {

    @Test
    fun getCacheKey_formatsTrackIdAndPlayingState() {
        val keyPlaying = WidgetArtRenderer.getCacheKey(42L, true)
        val keyPaused = WidgetArtRenderer.getCacheKey(42L, false)

        assertEquals("42_true", keyPlaying)
        assertEquals("42_false", keyPaused)
        assertNotEquals(keyPlaying, keyPaused)
    }

    @Test
    fun getCacheKey_differentiatesDifferentTracks() {
        val track1Key = WidgetArtRenderer.getCacheKey(101L, true)
        val track2Key = WidgetArtRenderer.getCacheKey(102L, true)

        assertNotEquals(track1Key, track2Key)
    }
}

