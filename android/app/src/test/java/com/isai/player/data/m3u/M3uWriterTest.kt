package com.isai.player.data.m3u

import com.isai.player.data.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream

class M3uWriterTest {

    private fun createTestTrack(id: Long, relativePath: String, title: String, artist: String, durationSec: Int): Track {
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
    fun write_formatsHeadersAndTracksCorrectly() {
        val tracks = listOf(
            createTestTrack(1L, "Rock/Stairway to Heaven.flac", "Stairway to Heaven", "Led Zeppelin", 482),
            createTestTrack(2L, "Rock/Bohemian Rhapsody.mp3", "Bohemian Rhapsody", "Queen", 354)
        )

        val output = M3uWriter.write("My Favorites", tracks)

        assertTrue(output.startsWith("#EXTM3U\n#PLAYLIST:My Favorites\n"))
        assertTrue(output.contains("#EXTINF:482,Led Zeppelin - Stairway to Heaven\nRock/Stairway to Heaven.flac\n"))
        assertTrue(output.contains("#EXTINF:354,Queen - Bohemian Rhapsody\nRock/Bohemian Rhapsody.mp3\n"))
    }

    @Test
    fun writeToStream_writesExactUtf8Bytes() {
        val tracks = listOf(
            createTestTrack(1L, "Track.mp3", "Title", "Artist", 100)
        )
        val stream = ByteArrayOutputStream()
        M3uWriter.writeToStream("Test", tracks, stream)

        val written = stream.toString("UTF-8")
        assertTrue(written.contains("#PLAYLIST:Test"))
        assertTrue(written.contains("#EXTINF:100,Artist - Title"))
        assertTrue(written.contains("Track.mp3"))
    }
}

