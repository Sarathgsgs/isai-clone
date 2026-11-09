package com.isai.player.ui.qa

import com.isai.player.data.lyrics.LrcParser
import com.isai.player.data.model.LyricsFormat
import com.isai.player.data.model.LyricsSource
import com.isai.player.ui.nowplaying.WaveformMath
import com.isai.player.ui.widget.WidgetArtRenderer
import com.isai.player.ui.widget.WidgetUpdater
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AddOnRound1ContractTest {

    @Test
    fun manifest_strictZeroNetworkAndWidgetContract() {
        val manifestCandidates = listOf(
            File("app/src/main/AndroidManifest.xml"),
            File("src/main/AndroidManifest.xml"),
            File("../app/src/main/AndroidManifest.xml")
        )
        val manifestFile = manifestCandidates.firstOrNull { it.exists() }
        assertNotNull("AndroidManifest.xml must exist", manifestFile)

        val manifestContent = manifestFile!!.readText()

        // 1. Zero Network Enforcement
        assertFalse(
            "Security violation: INTERNET permission must NEVER be present",
            manifestContent.contains("android.permission.INTERNET")
        )
        assertFalse(
            "Security violation: ACCESS_NETWORK_STATE permission must NEVER be present",
            manifestContent.contains("android.permission.ACCESS_NETWORK_STATE")
        )
        assertFalse(
            "Security violation: ACCESS_WIFI_STATE permission must NEVER be present",
            manifestContent.contains("android.permission.ACCESS_WIFI_STATE")
        )

        // 2. Widget Receiver Registration
        assertTrue(
            "Widget contract: IsaiGlanceWidgetReceiver must be registered in manifest",
            manifestContent.contains("IsaiGlanceWidgetReceiver")
        )
        assertTrue(
            "Widget contract: APPWIDGET_UPDATE action filter must be present",
            manifestContent.contains("android.appwidget.action.APPWIDGET_UPDATE")
        )
    }

    @Test
    fun waveform_deterministicFallbackContract() {
        val peaks1 = WaveformMath.generateSinusoidalWaveform(trackId = 999L, targetCount = 200)
        val peaks2 = WaveformMath.generateSinusoidalWaveform(trackId = 999L, targetCount = 200)
        val peaksOther = WaveformMath.generateSinusoidalWaveform(trackId = 1000L, targetCount = 200)

        assertEquals("Fallback must produce exact target bucket count", 200, peaks1.size)
        assertEquals("Fallback must be completely deterministic for identical trackId", peaks1, peaks2)
        assertNotEquals("Different trackIds must produce distinct waveform patterns", peaks1, peaksOther)

        peaks1.forEach { peak ->
            assertTrue("Peak amplitude must be >= 0.05", peak >= 0.05f)
            assertTrue("Peak amplitude must be <= 1.0", peak <= 1.0f)
        }
    }

    @Test
    fun lyrics_parserAndBinarySearchContract() {
        val lrc = """
            [ti:Test Title]
            [ar:Test Artist]
            [00:01.00]First line
            [00:05.50]Second line
            [00:10.00]Third line
        """.trimIndent()

        val parsed = LrcParser.parse(lrc)
        assertEquals(LyricsFormat.SYNCED, parsed.format)
        assertEquals(3, parsed.lines.size)

        // Binary search lookup contract
        assertEquals(0, LrcParser.findActiveLineIndex(parsed.lines, 0L))
        assertEquals(0, LrcParser.findActiveLineIndex(parsed.lines, 1000L))
        assertEquals(0, LrcParser.findActiveLineIndex(parsed.lines, 3000L))
        assertEquals(1, LrcParser.findActiveLineIndex(parsed.lines, 5500L))
        assertEquals(1, LrcParser.findActiveLineIndex(parsed.lines, 9999L))
        assertEquals(2, LrcParser.findActiveLineIndex(parsed.lines, 10_000L))
        assertEquals(2, LrcParser.findActiveLineIndex(parsed.lines, 60_000L))
    }

    @Test
    fun lyrics_sourcePriorityHierarchyContract() {
        val sources = listOf(LyricsSource.IMPORTED, LyricsSource.SIDECAR, LyricsSource.EMBEDDED)
        assertEquals(LyricsSource.IMPORTED, sources.minByOrNull { it.ordinal })
        assertTrue(LyricsSource.IMPORTED.ordinal < LyricsSource.SIDECAR.ordinal)
        assertTrue(LyricsSource.SIDECAR.ordinal < LyricsSource.EMBEDDED.ordinal)
    }

    @Test
    fun widget_cachingAndThrottlingContract() {
        // Art Cache Key
        val keyPlay = WidgetArtRenderer.getCacheKey(42L, true)
        val keyPause = WidgetArtRenderer.getCacheKey(42L, false)
        assertNotEquals(keyPlay, keyPause)
        assertEquals("42_true", keyPlay)

        // Updater Throttling
        val updater = WidgetUpdater(mockk(relaxed = true), mockk(relaxed = true))

        // State changes trigger immediate update
        assertTrue(updater.shouldTriggerUpdate(100L, 1L, true, 2L, true, 90L))
        assertTrue(updater.shouldTriggerUpdate(100L, 1L, true, 1L, false, 90L))

        // Identical state within 10s is throttled
        assertFalse(updater.shouldTriggerUpdate(5000L, 1L, true, 1L, true, 1000L))

        // Identical state after >= 10s is allowed
        assertTrue(updater.shouldTriggerUpdate(11_000L, 1L, true, 1L, true, 1000L))
    }
}

