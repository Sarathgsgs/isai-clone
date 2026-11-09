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
class ArtistDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var libraryRepository: LibraryRepository
    private lateinit var favoritesRepository: FavoritesRepository
    private lateinit var playerController: PlayerController

    private fun createSampleTrack(id: Long, album: String): Track = Track(
        id = id,
        uri = "uri:$id",
        title = "Track $id",
        titleSort = "track $id",
        artist = "A. R. Rahman",
        albumArtist = "A. R. Rahman",
        album = album,
        year = 1995,
        trackNo = 1,
        discNo = 1,
        durationMs = 220000L,
        mimeType = "audio/mpeg",
        sizeBytes = 1000L,
        lastModified = 0L,
        artPath = "/art/$album.jpg",
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
            createSampleTrack(1L, "Roja"),
            createSampleTrack(2L, "Bombay"),
            createSampleTrack(3L, "Roja")
        )

        every { libraryRepository.observeTracksForArtist("A. R. Rahman") } returns flowOf(tracks)
        every { favoritesRepository.observeFavoriteTrackIds() } returns flowOf(setOf(1L))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadsArtistTracksAndAlbums() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf("artistKey" to "A. R. Rahman"))
        val viewModel = ArtistDetailViewModel(savedStateHandle, libraryRepository, playerController, favoritesRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("A. R. Rahman", state.artistName)
        assertEquals(3, state.tracks.size)
        assertEquals(2, state.albums.size) // Roja and Bombay
    }

    @Test
    fun playbackActionsDelegate() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf("artistKey" to "A. R. Rahman"))
        val viewModel = ArtistDetailViewModel(savedStateHandle, libraryRepository, playerController, favoritesRepository)
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

