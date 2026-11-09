package com.isai.player.manifest

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Architectural contract test ensuring that Isai remains 100% offline.
 * Under no circumstances may any network permissions be declared in AndroidManifest.xml.
 */
class ManifestOfflineContractTest {

    private fun findManifestFile(): File {
        val candidates = listOf(
            File("src/main/AndroidManifest.xml"),
            File("app/src/main/AndroidManifest.xml"),
            File("../app/src/main/AndroidManifest.xml")
        )
        return candidates.firstOrNull { it.exists() }
            ?: error("AndroidManifest.xml not found in candidate paths: ${candidates.map { it.absolutePath }}")
    }

    @Test
    fun manifestContainsStrictlyNoNetworkPermissions() {
        val manifest = findManifestFile()
        val text = manifest.readText()

        // Verify zero network permissions
        assertFalse(
            "CRITICAL: android.permission.INTERNET must never be present in Isai manifest",
            text.contains("android.permission.INTERNET")
        )
        assertFalse(
            "CRITICAL: android.permission.ACCESS_NETWORK_STATE must never be present in Isai manifest",
            text.contains("android.permission.ACCESS_NETWORK_STATE")
        )
        assertFalse(
            "CRITICAL: android.permission.ACCESS_WIFI_STATE must never be present in Isai manifest",
            text.contains("android.permission.ACCESS_WIFI_STATE")
        )
        assertFalse(
            "CRITICAL: android.permission.CHANGE_NETWORK_STATE must never be present in Isai manifest",
            text.contains("android.permission.CHANGE_NETWORK_STATE")
        )
    }

    @Test
    fun manifestDeclaresRequiredLocalPlaybackPermissions() {
        val manifest = findManifestFile()
        val text = manifest.readText()

        assertTrue("Manifest must declare POST_NOTIFICATIONS", text.contains("android.permission.POST_NOTIFICATIONS"))
        assertTrue("Manifest must declare FOREGROUND_SERVICE", text.contains("android.permission.FOREGROUND_SERVICE"))
        assertTrue("Manifest must declare FOREGROUND_SERVICE_MEDIA_PLAYBACK", text.contains("android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK"))
        assertTrue("Manifest must declare WAKE_LOCK", text.contains("android.permission.WAKE_LOCK"))
    }
}

