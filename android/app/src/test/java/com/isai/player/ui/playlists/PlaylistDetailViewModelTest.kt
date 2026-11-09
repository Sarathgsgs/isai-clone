package com.isai.player.ui.playlists

import androidx.lifecycle.SavedStateHandle
import com.isai.player.data.model.Playlist
import com.isai.player.data.model.Track
import com.isai.player.data.repository.FavoritesRepository
import com.isai.player.data.repository.LibraryRepository
import com.isai.player.data.repository.PlaylistRepository
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlaylistDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var playlistRepository: PlaylistRepository
    private lateinit var libraryRepository: LibraryRepository
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
        playlistRepository = mockk(relaxed = true)
        libraryRepository = mockk(relaxed = true)
        favoritesRepository = mockk(relaxed = true)
        playerController = mockk(relaxed = true)

        val playlist = Playlist(id = 10L, name = "Chill Vibes", trackCount = 2)
        val playlistTracks = listOf(createSampleTrack(1L), createSampleTrack(2L))
        val allTracks = listOf(createSampleTrack(1L), createSampleTrack(2L), createSampleTrack(3L))

        every { playlistRepository.observePlaylistById(10L) } returns flowOf(playlist)
        every { playlistRepository.observeTracksForPlaylist(10L) } returns flowOf(playlistTracks)
        every { libraryRepository.observeTracks() } returns flowOf(allTracks)
        every { favoritesRepository.observeFavoriteTrackIds() } returns flowOf(setOf(1L))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadsPlaylistAndTracks() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf("playlistId" to 10L))
        val viewModel = PlaylistDetailViewModel(
            savedStateHandle,
            playlistRepository,
            libraryRepository,
            favoritesRepository,
            playerController
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Chill Vibes", state.playlist?.name)
        assertEquals(2, state.tracks.size)
        assertEquals(3, state.allLibraryTracks.size)
        assertEquals(360000L, state.totalDurationMs)
        assertEquals(false, state.isLoading)
    }

    @Test
    fun playbackActionsDelegate() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf("playlistId" to 10L))
        val viewModel = PlaylistDetailViewModel(
            savedStateHandle,
            playlistRepository,
            libraryRepository,
            favoritesRepository,
            playerController
        )
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
    }

    @Test
    fun playlistCrudActionsDelegate() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf("playlistId" to 10L))
        val viewModel = PlaylistDetailViewModel(
            savedStateHandle,
            playlistRepository,
            libraryRepository,
            favoritesRepository,
            playerController
        )
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.renamePlaylist("Party Hits")
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { playlistRepository.renamePlaylist(10L, "Party Hits") }

        viewModel.addTracksToPlaylist(listOf(3L))
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { playlistRepository.addTracksToPlaylist(10L, listOf(3L)) }

        viewModel.removeTrackFromPlaylist(1L)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { playlistRepository.removeTrackFromPlaylist(10L, 1L) }

        var deletedCalled = false
        viewModel.deletePlaylist { deletedCalled = true }
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { playlistRepository.deletePlaylist(10L) }
        assertTrue(deletedCalled)
    }
}

