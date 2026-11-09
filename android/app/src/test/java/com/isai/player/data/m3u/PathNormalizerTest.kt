package com.isai.player.data.m3u

import org.junit.Assert.assertEquals
import org.junit.Test

class PathNormalizerTest {

    @Test
    fun normalize_windowsBackslashesAndDriveLetter_normalizesCleanly() {
        val input = """C:\Music\Pink Floyd\Dark Side\01 Speak to Me.flac"""
        val normalized = PathNormalizer.normalize(input)
        assertEquals("Music/Pink Floyd/Dark Side/01 Speak to Me.flac", normalized)
    }

    @Test
    fun normalize_urlEncodedPath_decodesPercentTokens() {
        val input = "file:///storage/emulated/0/Music/The%20Beatles/Abbey%20Road/01%20Come%20Together.mp3"
        val normalized = PathNormalizer.normalize(input)
        assertEquals("storage/emulated/0/Music/The Beatles/Abbey Road/01 Come Together.mp3", normalized)
    }

    @Test
    fun normalize_relativeDotSlashAndRedundantSlashes_cleaned() {
        val input = "./Albums//Rock///Song.ogg"
        val normalized = PathNormalizer.normalize(input)
        assertEquals("Albums/Rock/Song.ogg", normalized)
    }

    @Test
    fun normalizeForSearch_returnsLowercaseString() {
        val input = "Queen/A Night at the Opera/Bohemian Rhapsody.mp3"
        val result = PathNormalizer.normalizeForSearch(input)
        assertEquals("queen/a night at the opera/bohemian rhapsody.mp3", result)
    }

    @Test
    fun getFileName_extractsFileNameOnly() {
        val input = "Pink Floyd/Dark Side/01 Speak to Me.flac"
        assertEquals("01 Speak to Me.flac", PathNormalizer.getFileName(input))
    }

    @Test
    fun getTrailingSegments_returnsTargetCountOfSegments() {
        val input = "Music/External/Rock/Pink Floyd/01 Speak to Me.flac"
        val trailing2 = PathNormalizer.getTrailingSegments(input, 2)
        assertEquals("Pink Floyd/01 Speak to Me.flac", trailing2)

        val trailing3 = PathNormalizer.getTrailingSegments(input, 3)
        assertEquals("Rock/Pink Floyd/01 Speak to Me.flac", trailing3)
    }
}

