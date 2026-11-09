package com.isai.player.data.m3u

import com.isai.player.data.model.Track
import org.junit.Assert.assertEquals
import org.junit.Test

class M3uFixturesCompatibilityTest {

    private fun createTestTrack(
        id: Long,
        relativePath: String,
        title: String,
        artist: String,
        durationSec: Int
    ): Track {
        return Track(
            id = id,
            uri = "content://music/$id",
            title = title,
            titleSort = title,
            artist = artist,
            albumArtist = artist,
            album = "Album",
            year = 2024,
            trackNo = 1,
            discNo = 1,
            durationMs = durationSec * 1000L,
            mimeType = "audio/mpeg",
            sizeBytes = 4000000L,
            lastModified = 1000L,
            artPath = null,
            folderPath = "Folder",
            dateAdded = 1000L,
            relativePath = relativePath
        )
    }

    @Test
    fun foobar2000_windowsBackslashFixture_parsesAndMatches() {
        val fixture = """
            #EXTM3U
            #EXTINF:234,Pink Floyd - Time
            Pink Floyd\The Dark Side of the Moon\04 Time.flac
            #EXTINF:412,Pink Floyd - Money
            Pink Floyd\The Dark Side of the Moon\06 Money.flac
        """.trimIndent()

        val parsed = M3uParser.parse(fixture)
        assertEquals(2, parsed.entries.size)

        val library = listOf(
            createTestTrack(1L, "Pink Floyd/The Dark Side of the Moon/04 Time.flac", "Time", "Pink Floyd", 234),
            createTestTrack(2L, "Pink Floyd/The Dark Side of the Moon/06 Money.flac", "Money", "Pink Floyd", 412)
        )

        val matched = TrackMatcher.matchEntries(parsed.entries, library)
        assertEquals(2, matched.matchedCount)
        assertEquals(0, matched.notFoundCount)
    }

    @Test
    fun vlc_fileUriAndPercentEncodingFixture_parsesAndMatches() {
        val fixture = """
            #EXTM3U
            #EXTINF:180,The Beatles - Come Together
            file:///C:/Users/User/Music/The%20Beatles/Abbey%20Road/01%20Come%20Together.mp3
            #EXTINF:200,The Beatles - Something
            file:///C:/Users/User/Music/The%20Beatles/Abbey%20Road/02%20Something.mp3
        """.trimIndent()

        val parsed = M3uParser.parse(fixture)
        assertEquals(2, parsed.entries.size)

        val library = listOf(
            createTestTrack(3L, "The Beatles/Abbey Road/01 Come Together.mp3", "Come Together", "The Beatles", 180),
            createTestTrack(4L, "The Beatles/Abbey Road/02 Something.mp3", "Something", "The Beatles", 200)
        )

        val matched = TrackMatcher.matchEntries(parsed.entries, library)
        assertEquals(2, matched.matchedCount)
        assertEquals(0, matched.notFoundCount)
    }

    @Test
    fun musicBee_playlistTagAndRelativeSegments_parsesAndMatches() {
        val fixture = """
            #EXTM3U
            #PLAYLIST:Classics
            #EXTINF:215,Queen - Killer Queen
            ..\Rock\Queen\Sheer Heart Attack\02 Killer Queen.mp3
        """.trimIndent()

        val parsed = M3uParser.parse(fixture)
        assertEquals("Classics", parsed.name)
        assertEquals(1, parsed.entries.size)

        val library = listOf(
            createTestTrack(5L, "Queen/Sheer Heart Attack/02 Killer Queen.mp3", "Killer Queen", "Queen", 215)
        )

        val matched = TrackMatcher.matchEntries(parsed.entries, library)
        assertEquals(1, matched.matchedCount)
    }

    @Test
    fun poweramp_androidAbsoluteStoragePath_parsesAndMatches() {
        val fixture = """
            #EXTM3U
            #EXTINF:354,Bohemian Rhapsody
            /storage/emulated/0/Music/Queen/Bohemian Rhapsody.mp3
        """.trimIndent()

        val parsed = M3uParser.parse(fixture)
        assertEquals(1, parsed.entries.size)

        val library = listOf(
            createTestTrack(6L, "Queen/Bohemian Rhapsody.mp3", "Bohemian Rhapsody", "Queen", 354)
        )

        val matched = TrackMatcher.matchEntries(parsed.entries, library)
        assertEquals(1, matched.matchedCount)
    }
}

