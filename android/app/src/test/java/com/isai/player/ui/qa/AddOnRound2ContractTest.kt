package com.isai.player.ui.qa

import com.isai.player.data.equalizer.ClippingMath
import com.isai.player.data.equalizer.EqPreset
import com.isai.player.data.equalizer.EqPresetResampler
import com.isai.player.data.m3u.M3uWriter
import com.isai.player.data.m3u.PathNormalizer
import com.isai.player.data.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AddOnRound2ContractTest {

    private fun findFile(relativePath: String): File? {
        val candidates = listOf(
            File(relativePath),
            File("app/$relativePath"),
            File("../$relativePath"),
            File("../app/$relativePath")
        )
        return candidates.firstOrNull { it.exists() }
    }

    @Test
    fun manifest_strictZeroNetworkAndAutomotiveContract() {
        val manifestFile = findFile("src/main/AndroidManifest.xml")
            ?: findFile("AndroidManifest.xml")
        assertNotNull("AndroidManifest.xml must exist", manifestFile)

        val text = manifestFile!!.readText()

        // 1. Strict Zero Network Verification
        assertFalse("CRITICAL: android.permission.INTERNET must NEVER be present", text.contains("android.permission.INTERNET"))
        assertFalse("CRITICAL: android.permission.ACCESS_NETWORK_STATE must NEVER be present", text.contains("android.permission.ACCESS_NETWORK_STATE"))
        assertFalse("CRITICAL: android.permission.ACCESS_WIFI_STATE must NEVER be present", text.contains("android.permission.ACCESS_WIFI_STATE"))

        // 2. Automotive & MediaLibraryService Verification
        assertTrue("Manifest must declare automotive app descriptor metadata", text.contains("com.google.android.gms.car.application"))
        assertTrue("Manifest must declare MediaLibraryService action", text.contains("androidx.media3.session.MediaLibraryService"))
        assertTrue("Manifest must register CarArtworkProvider", text.contains("CarArtworkProvider"))

        // 3. Automotive XML verification
        val autoXml = findFile("src/main/res/xml/automotive_app_desc.xml")
        assertNotNull("automotive_app_desc.xml must exist", autoXml)
        assertTrue("automotive_app_desc must declare media capability", autoXml!!.readText().contains("<uses name=\"media\""))
    }

    @Test
    fun equalizer_headroomAndResamplingContract() {
        // Clipping auto-attenuation
        val analysis = ClippingMath.analyze(
            preampDb = 4.0f,
            bandGainsMilliBels = listOf(200, 400, 0, -100),
            preventClipping = true
        )
        // Max boost = 4.0 + 4.0 = 8.0 dB
        assertTrue(analysis.isClippingPotential)
        assertEquals(-8.0f, analysis.attenuationDb, 0.01f)
        assertEquals(-4.0f, analysis.effectivePreampDb, 0.01f)

        // Resampling
        val preset = EqPreset(name = "Test", referenceFreqsHz = listOf(100, 1000), referenceGainsDb = listOf(5f, -5f))
        val resampled = EqPresetResampler.resample(preset, listOf(100, 1000))
        assertEquals(500, resampled[0])
        assertEquals(-500, resampled[1])
    }

    @Test
    fun m3u_normalizationAndSerializationContract() {
        // Path normalizer
        val normalized = PathNormalizer.normalize("""D:\Music\Artists\Song.flac""")
        assertEquals("Music/Artists/Song.flac", normalized)

        // Writer
        val track = Track(
            id = 10L,
            uri = "content://music/10",
            title = "Test Song",
            titleSort = "Test Song",
            artist = "Test Artist",
            albumArtist = "Test Artist",
            album = "Test Album",
            year = 2024,
            trackNo = 1,
            discNo = 1,
            durationMs = 180_000L,
            mimeType = "audio/flac",
            sizeBytes = 20_000_000L,
            lastModified = 1000L,
            artPath = null,
            folderPath = "Folder",
            dateAdded = 1000L,
            relativePath = "Artists/Song.flac"
        )
        val m3u = M3uWriter.write("TestPL", listOf(track))
        assertTrue(m3u.contains("#PLAYLIST:TestPL"))
        assertTrue(m3u.contains("#EXTINF:180,Test Artist - Test Song"))
        assertTrue(m3u.contains("Artists/Song.flac"))
    }
}

