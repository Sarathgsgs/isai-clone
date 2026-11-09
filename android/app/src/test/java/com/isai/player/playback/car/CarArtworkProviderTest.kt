package com.isai.player.playback.car

import android.net.Uri
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class CarArtworkProviderTest {

    @Test
    fun validateAndResolveTrackId_validNumericId_succeeds() {
        val uri = mockk<Uri>()
        every { uri.lastPathSegment } returns "42"
        val id = CarArtworkProvider.validateAndResolveTrackId(uri)
        assertEquals(42L, id)
    }

    @Test(expected = SecurityException::class)
    fun validateAndResolveTrackId_pathTraversalDotDot_throwsSecurityException() {
        val uri = mockk<Uri>()
        every { uri.lastPathSegment } returns "../etc/passwd"
        CarArtworkProvider.validateAndResolveTrackId(uri)
    }

    @Test(expected = SecurityException::class)
    fun validateAndResolveTrackId_pathTraversalSlash_throwsSecurityException() {
        val uri = mockk<Uri>()
        every { uri.lastPathSegment } returns "42/other"
        CarArtworkProvider.validateAndResolveTrackId(uri)
    }

    @Test(expected = SecurityException::class)
    fun validateAndResolveTrackId_pathTraversalBackslash_throwsSecurityException() {
        val uri = mockk<Uri>()
        every { uri.lastPathSegment } returns "42\\other"
        CarArtworkProvider.validateAndResolveTrackId(uri)
    }

    @Test(expected = IllegalArgumentException::class)
    fun validateAndResolveTrackId_nonNumericId_throwsIllegalArgumentException() {
        val uri = mockk<Uri>()
        every { uri.lastPathSegment } returns "invalid_id"
        CarArtworkProvider.validateAndResolveTrackId(uri)
    }
}

