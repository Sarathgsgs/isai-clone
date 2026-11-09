package com.isai.player.data.scanner

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MetadataExtractorTest {

    private lateinit var metadataExtractor: MetadataExtractor

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val artCache = ArtCache(context)
        metadataExtractor = MetadataExtractor(context, artCache)
    }

    @Test
    fun sanitizeFileNameToTitleStripsNumbersAndExtensions() {
        assertEquals("Sundari", metadataExtractor.sanitizeFileNameToTitle("01 - Sundari.mp3"))
        assertEquals("Chinna Chinna Aasai", metadataExtractor.sanitizeFileNameToTitle("02. Chinna Chinna Aasai.flac"))
        assertEquals("Nilaave Vaa", metadataExtractor.sanitizeFileNameToTitle("3_Nilaave_Vaa.wav"))
        assertEquals("Bohemian Rhapsody", metadataExtractor.sanitizeFileNameToTitle("Bohemian Rhapsody.opus"))
        assertEquals("Track Title", metadataExtractor.sanitizeFileNameToTitle("04 Track Title.m4a"))
        assertEquals("Song With Multiple Words", metadataExtractor.sanitizeFileNameToTitle("Song_With_Multiple_Words.aac"))
    }

    @Test
    fun sanitizeForSortStripsLeadingArticles() {
        assertEquals("beatles", metadataExtractor.sanitizeForSort("The Beatles"))
        assertEquals("hard day's night", metadataExtractor.sanitizeForSort("A Hard Day's Night"))
        assertEquals("ending", metadataExtractor.sanitizeForSort("An Ending"))
        assertEquals("ilaiyaraaja", metadataExtractor.sanitizeForSort("Ilaiyaraaja"))
        assertEquals("a. r. rahman", metadataExtractor.sanitizeForSort("A. R. Rahman"))
    }

    @Test
    fun parseCompoundNumberSplitsTrackAndDiscCorrectly() {
        assertEquals(3, metadataExtractor.parseCompoundNumber("3/12"))
        assertEquals(4, metadataExtractor.parseCompoundNumber("04"))
        assertEquals(1, metadataExtractor.parseCompoundNumber("1-10"))
        assertEquals(2, metadataExtractor.parseCompoundNumber("2, 5"))
        assertEquals(1, metadataExtractor.parseCompoundNumber(null, default = 1))
        assertEquals(1, metadataExtractor.parseCompoundNumber("", default = 1))
        assertEquals(1, metadataExtractor.parseCompoundNumber("invalid", default = 1))
    }

    @Test
    fun parseYearExtractsFourDigitYear() {
        assertEquals(1991, metadataExtractor.parseYear("1991"))
        assertEquals(2024, metadataExtractor.parseYear("2024-05-14"))
        assertEquals(1986, metadataExtractor.parseYear("1986/11"))
        assertEquals(1995, metadataExtractor.parseYear("Released: 1995"))
        assertEquals(0, metadataExtractor.parseYear(null))
        assertEquals(0, metadataExtractor.parseYear("Unknown"))
    }
}

