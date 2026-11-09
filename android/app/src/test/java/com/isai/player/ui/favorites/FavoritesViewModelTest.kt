package com.isai.player.ui.favorites

import com.isai.player.data.model.Track
import com.isai.player.data.repository.FavoritesRepository
import com.isai.player.playback.PlayerController
import io.mockk.coVerify
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
class FavoritesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var favoritesRepository: FavoritesRepository
    private lateinit var playerController: PlayerController

    private fun createSampleTrack(id: Long = 1L): Track = Track(
        id = id,
        uri = "uri:$id",
        title = "Track $id",
        titleSort = "track $id",
        artist = "Artist",
        albumArtist = "Artist",
        album = "Album",
        year = 2024,
        trackNo = 1,
        discNo = 1,
        durationMs = 150000L,
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
        favoritesRepository = mockk(relaxed = true)
        playerController = mockk(relaxed = true)

        val favorites = listOf(createSampleTrack(1L), createSampleTrack(2L))
        every { favoritesRepository.observeFavoriteTracks() } returns flowOf(favorites)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadsFavoriteTracksAndTotalDuration() = runTest {
        val viewModel = FavoritesViewModel(favoritesRepository, playerController)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.tracks.size)
        assertEquals(300000L, state.totalDurationMs)
        assertEquals(false, state.isLoading)
    }

    @Test
    fun playbackActionsDelegate() = runTest {
        val viewModel = FavoritesViewModel(favoritesRepository, playerController)
        testDispatcher.scheduler.advanceUntilIdle()

        val track = viewModel.uiState.value.tracks.first()
        viewModel.playTrack(track)
        verify { playerController.playTrack(track, any(), 0) }

        viewModel.playAll()
        verify { playerController.playTracks(viewModel.uiState.value.tracks, 0) }

        viewModel.shuffleAll()
        verify { playerController.playTracks(any(), 0) }

        viewModel.playNext(track)
        verify { playerController.playNext(track) }

        viewModel.addToQueue(track)
        verify { playerController.addToQueue(listOf(track)) }

        viewModel.toggleFavorite(track.id)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { favoritesRepository.toggleFavorite(track.id) }
    }
}

