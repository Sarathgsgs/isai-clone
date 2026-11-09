package com.isai.player.data.db.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class Fnv1aTest {

    @Test
    fun hashIsDeterministicForSameInput() {
        val uri = "content://com.android.externalstorage.documents/document/primary%3AMusic%2FThalapathi%2FSundari.mp3"
        val hash1 = Fnv1a.hash64(uri)
        val hash2 = Fnv1a.hash64(uri)

        assertEquals(hash1, hash2)
    }

    @Test
    fun differentInputsProduceDifferentHashes() {
        val uri1 = "content://com.android.externalstorage.documents/document/primary%3AMusic%2FTrack1.mp3"
        val uri2 = "content://com.android.externalstorage.documents/document/primary%3AMusic%2FTrack2.mp3"

        assertNotEquals(Fnv1a.hash64(uri1), Fnv1a.hash64(uri2))
    }

    @Test
    fun emptyStringProducesOffsetBasis() {
        // FNV-1a 64-bit offset basis is 0xcbf29ce484222325L (-3750763034362895579L in signed 64-bit)
        val expectedOffsetBasis = -0x340d631b7bdddcdbL
        assertEquals(expectedOffsetBasis, Fnv1a.hash64(""))
    }

    @Test
    fun handlesUnicodeCharactersDeterministically() {
        val tamilTitle = "content://media/external/audio/சுந்தரி_கண்ணால்_ஒரு_சேதி.flac"
        val hash1 = Fnv1a.hash64(tamilTitle)
        val hash2 = Fnv1a.hash64(tamilTitle)

        assertEquals(hash1, hash2)
        assertNotEquals(0L, hash1)
    }

    @Test
    fun handlesVeryLongUriStrings() {
        val longUri = "content://com.android.providers.media.documents/tree/1234-5678%3AMusic/document/" +
            "1234-5678%3AMusic%2FVery%2FLong%2FPath%2FTo%2FSubfolder%2FWith%2FMany%2FNested%2FDirectories%2F" +
            "Album%20Name%20(2024)%20[FLAC%2024bit%2096kHz]%2F01.%20Artist%20-%20Super%20Track%20Title%20(Remastered).flac"

        val hash = Fnv1a.hash64(longUri)
        assertNotEquals(0L, hash)
        assertEquals(hash, Fnv1a.hash64(longUri))
    }
}

