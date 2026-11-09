package com.isai.player.ui.detail

import androidx.lifecycle.SavedStateHandle
import com.isai.player.data.model.Track
import com.isai.player.data.repository.FavoritesRepository
import com.isai.player.data.repository.LibraryRepository
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
class AlbumDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var libraryRepository: LibraryRepository
    private lateinit var favoritesRepository: FavoritesRepository
    private lateinit var playerController: PlayerController

    private fun createSampleTrack(id: Long, discNo: Int, trackNo: Int): Track = Track(
        id = id,
        uri = "uri:$id",
        title = "Track $id",
        titleSort = "track $id",
        artist = "Artist 1",
        albumArtist = "Artist 1",
        album = "Roja",
        year = 1992,
        trackNo = trackNo,
        discNo = discNo,
        durationMs = 200000L,
        mimeType = "audio/mpeg",
        sizeBytes = 1000L,
        lastModified = 0L,
        artPath = "/art/roja.jpg",
        folderPath = "Music",
        dateAdded = 0L
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        libraryRepository = mockk(relaxed = true)
        favoritesRepository = mockk(relaxed = true)
        playerController = mockk(relaxed = true)

        val tracks = listOf(
            createSampleTrack(1L, discNo = 1, trackNo = 1),
            createSampleTrack(2L, discNo = 1, trackNo = 2),
            createSampleTrack(3L, discNo = 2, trackNo = 1)
        )

        every { libraryRepository.observeTracksForAlbum("Roja") } returns flowOf(tracks)
        every { favoritesRepository.observeFavoriteTrackIds() } returns flowOf(setOf(1L))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadsAlbumTracksAndDiscGroups() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf("albumKey" to "Roja"))
        val viewModel = AlbumDetailViewModel(savedStateHandle, libraryRepository, playerController, favoritesRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Roja", state.albumTitle)
        assertEquals("Artist 1", state.albumArtist)
        assertEquals(3, state.tracks.size)
        assertEquals(2, state.discGroups.size)
        assertEquals(400000L + 200000L, state.totalDurationMs)
        assertEquals(1992, state.releaseYear)
    }

    @Test
    fun playbackActionsDelegate() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf("albumKey" to "Roja"))
        val viewModel = AlbumDetailViewModel(savedStateHandle, libraryRepository, playerController, favoritesRepository)
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

