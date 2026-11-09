package com.isai.player.data.m3u

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class M3uParserTest {

    @Test
    fun parse_standardExtM3u_parsesPlaylistAndEntries() {
        val content = """
            #EXTM3U
            #PLAYLIST:Rock Classics
            #EXTINF:245,Led Zeppelin - Stairway to Heaven
            Led Zeppelin/IV/04 Stairway to Heaven.flac
            #EXTINF:354,Queen - Bohemian Rhapsody
            Queen/A Night at the Opera/Bohemian Rhapsody.mp3
        """.trimIndent()

        val playlist = M3uParser.parse(content)
        assertEquals("Rock Classics", playlist.name)
        assertEquals(2, playlist.entries.size)

        val entry1 = playlist.entries[0]
        assertEquals("Led Zeppelin/IV/04 Stairway to Heaven.flac", entry1.normalizedPath)
        assertEquals(245, entry1.durationSeconds)
        assertEquals("Led Zeppelin", entry1.artist)
        assertEquals("Stairway to Heaven", entry1.title)

        val entry2 = playlist.entries[1]
        assertEquals("Queen/A Night at the Opera/Bohemian Rhapsody.mp3", entry2.normalizedPath)
        assertEquals(354, entry2.durationSeconds)
        assertEquals("Queen", entry2.artist)
        assertEquals("Bohemian Rhapsody", entry2.title)
    }

    @Test
    fun parse_simpleM3uWithoutExtInf_parsesPaths() {
        val content = """
            Track1.mp3
            Track2.flac
            Track3.ogg
        """.trimIndent()

        val playlist = M3uParser.parse(content)
        assertNull(playlist.name)
        assertEquals(3, playlist.entries.size)
        assertEquals("Track1.mp3", playlist.entries[0].normalizedPath)
        assertNull(playlist.entries[0].durationSeconds)
        assertNull(playlist.entries[0].artist)
    }

    @Test
    fun parse_utf8WithBom_stripsBomCleanly() {
        val bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
        val textBytes = "#EXTM3U\nSong.mp3".toByteArray(Charsets.UTF_8)
        val combined = bom + textBytes

        val playlist = M3uParser.parse(combined)
        assertEquals(1, playlist.entries.size)
        assertEquals("Song.mp3", playlist.entries[0].normalizedPath)
    }

    @Test
    fun parse_skipsCommentsAndBlankLines() {
        val content = """
            #EXTM3U
            # Some random comment
            
            #EXTINF:120,Artist - Title
            # Another comment line
            Song.mp3
            
        """.trimIndent()

        val playlist = M3uParser.parse(content)
        assertEquals(1, playlist.entries.size)
        assertEquals("Song.mp3", playlist.entries[0].normalizedPath)
    }
}

