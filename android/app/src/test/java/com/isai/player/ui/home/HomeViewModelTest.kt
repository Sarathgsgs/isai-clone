package com.isai.player.ui.home

import com.isai.player.data.model.Album
import com.isai.player.data.model.Artist
import com.isai.player.data.model.Track
import com.isai.player.data.repository.HistoryRepository
import com.isai.player.data.repository.LibraryRepository
import com.isai.player.playback.PlayerController
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var libraryRepository: LibraryRepository
    private lateinit var historyRepository: HistoryRepository
    private lateinit var playerController: PlayerController

    private fun createSampleTrack(id: Long = 1L): Track = Track(
        id = id,
        uri = "uri:$id",
        title = "Track $id",
        titleSort = "track $id",
        artist = "Artist $id",
        albumArtist = "Artist $id",
        album = "Album $id",
        year = 2024,
        trackNo = 1,
        discNo = 1,
        durationMs = 180000L,
        mimeType = "audio/mpeg",
        sizeBytes = 1000L,
        lastModified = 0L,
        artPath = null,
        folderPath = "Music",
        dateAdded = 0L
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        libraryRepository = mockk(relaxed = true)
        historyRepository = mockk(relaxed = true)
        playerController = mockk(relaxed = true)

        val sampleTracks = listOf(createSampleTrack(1L), createSampleTrack(2L))
        every { historyRepository.observeRecentlyPlayed(20) } returns flowOf(sampleTracks)
        every { libraryRepository.observeTrackCount() } returns flowOf(12)
        every { libraryRepository.observeAlbums(any()) } returns flowOf(listOf(mockk<Album>(), mockk<Album>()))
        every { libraryRepository.observeArtists(any()) } returns flowOf(listOf(mockk<Artist>()))
        every { libraryRepository.observeTracks(any()) } returns flowOf(sampleTracks)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun recentlyPlayedLoadsFromRepository() = runTest {
        val viewModel = HomeViewModel(libraryRepository, historyRepository, playerController)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, viewModel.recentlyPlayed.value.size)
        assertEquals(1L, viewModel.recentlyPlayed.value[0].id)
        assertEquals(12, viewModel.totalSongsCount.value)
        assertEquals(2, viewModel.albumsCount.value)
        assertEquals(1, viewModel.artistsCount.value)
    }

    @Test
    fun playTrackDelegatesToPlayerController() = runTest {
        val viewModel = HomeViewModel(libraryRepository, historyRepository, playerController)
        val track = createSampleTrack(1L)
        val queue = listOf(track)

        viewModel.playTrack(track, queue)
        verify { playerController.playTrack(track, queue, 0) }
    }

    @Test
    fun shuffleAllShufflesLibraryAndPlays() = runTest {
        val viewModel = HomeViewModel(libraryRepository, historyRepository, playerController)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.shuffleAll()
        testDispatcher.scheduler.advanceUntilIdle()

        verify { playerController.playTracks(any(), 0) }
    }
}

