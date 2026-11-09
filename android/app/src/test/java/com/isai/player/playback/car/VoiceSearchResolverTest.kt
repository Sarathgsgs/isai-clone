package com.isai.player.playback.car

import com.isai.player.data.db.dao.AlbumDao
import com.isai.player.data.db.dao.ArtistDao
import com.isai.player.data.db.dao.TrackDao
import com.isai.player.data.db.entity.TrackEntity
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class VoiceSearchResolverTest {

    private val trackDao = mockk<TrackDao>(relaxed = true)
    private val albumDao = mockk<AlbumDao>(relaxed = true)
    private val artistDao = mockk<ArtistDao>(relaxed = true)
    private lateinit var resolver: VoiceSearchResolver

    @Before
    fun setUp() {
        resolver = VoiceSearchResolver(trackDao, albumDao, artistDao)
    }

    private fun createDummyTrackEntity(id: Long, title: String, artist: String, album: String): TrackEntity {
        return TrackEntity(
            id = id,
            uri = "content://music/$id",
            title = title,
            titleSort = title,
            artist = artist,
            album = album,
            albumArtist = artist,
            durationMs = 200_000L,
            trackNo = 1,
            discNo = 1,
            year = 2024,
            genre = "Rock",
            folderPath = "/storage/Music",
            artPath = null,
            dateAdded = 1000L,
            dateModified = 1000L,
            sizeBytes = 5_000_000L,
            isMissing = false
        )
    }

    @Test
    fun resolveSearch_blankQuery_callsGetRandomTracks() = runTest {
        val dummy = listOf(createDummyTrackEntity(1L, "Song", "Artist", "Album"))
        coEvery { trackDao.getRandomTracks(any()) } returns dummy

        val result = resolver.resolveSearch("")
        assertEquals(1, result.size)
        assertEquals("Song", result[0].title)
    }

    @Test
    fun resolveSearch_artistMatch_returnsArtistTracks() = runTest {
        val dummy = listOf(createDummyTrackEntity(2L, "BeatlesSong", "The Beatles", "Abbey Road"))
        coEvery { artistDao.getTracksForArtistSync("The Beatles") } returns dummy

        val result = resolver.resolveSearch("The Beatles")
        assertEquals(1, result.size)
        assertEquals("BeatlesSong", result[0].title)
    }

    @Test
    fun resolveSearch_albumMatch_returnsAlbumTracks() = runTest {
        val dummy = listOf(createDummyTrackEntity(3L, "Come Together", "The Beatles", "Abbey Road"))
        coEvery { artistDao.getTracksForArtistSync("Abbey Road") } returns emptyList()
        coEvery { albumDao.getTracksForAlbumSync("Abbey Road") } returns dummy

        val result = resolver.resolveSearch("Abbey Road")
        assertEquals(1, result.size)
        assertEquals("Come Together", result[0].title)
    }

    @Test
    fun resolveSearch_generalSearch_returnsSearchResults() = runTest {
        val dummy = listOf(createDummyTrackEntity(4L, "Yesterday", "The Beatles", "Help!"))
        coEvery { artistDao.getTracksForArtistSync("Yesterday") } returns emptyList()
        coEvery { albumDao.getTracksForAlbumSync("Yesterday") } returns emptyList()
        coEvery { trackDao.searchTracks("Yesterday") } returns flowOf(dummy)

        val result = resolver.resolveSearch("Yesterday")
        assertEquals(1, result.size)
        assertEquals("Yesterday", result[0].title)
    }
}

