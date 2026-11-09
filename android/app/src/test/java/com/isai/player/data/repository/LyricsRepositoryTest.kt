package com.isai.player.data.repository

import android.content.Context
import com.isai.player.data.db.dao.LyricsDao
import com.isai.player.data.db.entity.LyricsEntity
import com.isai.player.data.lyrics.LyricsFormat
import com.isai.player.data.lyrics.LyricsSource
import com.isai.player.data.model.Track
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LyricsRepositoryTest {

    private val context: Context = mockk(relaxed = true)
    private val lyricsDao: LyricsDao = mockk(relaxed = true)
    private lateinit var repository: LyricsRepositoryImpl

    private fun sampleTrack(id: Long = 100L): Track = Track(
        id = id,
        uri = "content://media/audio/100",
        title = "Song Title",
        titleSort = "song title",
        artist = "Artist Name",
        albumArtist = "Artist Name",
        album = "Album Name",
        year = 2024,
        trackNo = 1,
        discNo = 1,
        durationMs = 200000L,
        mimeType = "audio/mpeg",
        sizeBytes = 5000L,
        lastModified = 0L,
        artPath = null,
        folderPath = "Music",
        dateAdded = 0L
    )

    @Before
    fun setUp() {
        repository = LyricsRepositoryImpl(context, lyricsDao)
    }

    @Test
    fun databaseHitReturnsImmediatelyAndPreservesOffset() = runTest {
        val track = sampleTrack(101L)
        val cachedEntity = LyricsEntity(
            trackId = track.id,
            source = LyricsSource.IMPORTED.name,
            format = LyricsFormat.SYNCED.name,
            content = "[00:05.00]Cached lyric line",
            offsetMs = 350,
            lastModified = 1000L
        )

        coEvery { lyricsDao.getLyrics(track.id) } returns cachedEntity

        val lyrics = repository.getOrDiscoverLyrics(track)
        assertNotNull(lyrics)
        assertEquals(LyricsFormat.SYNCED, lyrics?.format)
        assertEquals(350, lyrics?.offsetMs)
        assertEquals(1, lyrics?.lines?.size)
        assertEquals("Cached lyric line", lyrics?.lines?.first()?.text)

        // Verifies no insert occurred because cache was used
        coVerify(exactly = 0) { lyricsDao.insertLyrics(any()) }
    }

    @Test
    fun userImportPersistsAsImportedSource() = runTest {
        val trackId = 102L
        val rawLrc = "[00:10.00]Imported lyrics"

        repository.importLyrics(trackId, rawLrc)

        coVerify(exactly = 1) {
            lyricsDao.insertLyrics(
                match {
                    it.trackId == trackId &&
                            it.source == LyricsSource.IMPORTED.name &&
                            it.format == LyricsFormat.SYNCED.name &&
                            it.content == rawLrc
                }
            )
        }
    }

    @Test
    fun updateLyricsOffsetDelegatesToDao() = runTest {
        val trackId = 103L
        repository.updateLyricsOffset(trackId, 500)

        coVerify(exactly = 1) { lyricsDao.updateOffset(trackId, 500, any()) }
    }
}

