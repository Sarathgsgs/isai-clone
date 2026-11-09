package com.isai.player.ui.library

import com.isai.player.data.model.Album
import com.isai.player.data.model.Artist
import com.isai.player.data.model.Folder
import com.isai.player.data.model.Playlist
import com.isai.player.data.model.Track
import com.isai.player.data.model.TrackSortOrder
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var libraryRepository: LibraryRepository
    private lateinit var playlistRepository: PlaylistRepository
    private lateinit var favoritesRepository: FavoritesRepository
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
        playlistRepository = mockk(relaxed = true)
        favoritesRepository = mockk(relaxed = true)
        playerController = mockk(relaxed = true)

        val sampleTracks = listOf(createSampleTrack(1L), createSampleTrack(2L))
        every { libraryRepository.observeTracks(any()) } returns flowOf(sampleTracks)
        every { libraryRepository.observeAlbums(any()) } returns flowOf(listOf(mockk<Album>()))
        every { libraryRepository.observeArtists(any()) } returns flowOf(listOf(mockk<Artist>()))
        every { libraryRepository.observeFolders() } returns flowOf(listOf(mockk<Folder>()))
        every { playlistRepository.observeAllPlaylists() } returns flowOf(listOf(mockk<Playlist>()))
        every { favoritesRepository.observeFavoriteTrackIds() } returns flowOf(setOf(1L))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun tabSelectionUpdatesState() = runTest {
        val viewModel = LibraryViewModel(
            libraryRepository,
            playlistRepository,
            favoritesRepository,
            playerController
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, viewModel.selectedTabIndex.value)

        viewModel.selectTab(2)
        assertEquals(2, viewModel.selectedTabIndex.value)
    }

    @Test
    fun sortOrderUpdatesAndQueriesTracks() = runTest {
        val viewModel = LibraryViewModel(
            libraryRepository,
            playlistRepository,
            favoritesRepository,
            playerController
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(TrackSortOrder.TITLE_ASC, viewModel.sortOrder.value)

        viewModel.setSortOrder(TrackSortOrder.ARTIST_ASC)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(TrackSortOrder.ARTIST_ASC, viewModel.sortOrder.value)
        verify { libraryRepository.observeTracks(TrackSortOrder.ARTIST_ASC) }
    }

    @Test
    fun trackMenuActionsDelegateCorrectly() = runTest {
        val viewModel = LibraryViewModel(
            libraryRepository,
            playlistRepository,
            favoritesRepository,
            playerController
        )
        val track = createSampleTrack(1L)
        val queue = listOf(track)

        viewModel.playTrack(track, queue)
        verify { playerController.playTrack(track, queue, 0) }

        viewModel.playNext(track)
        verify { playerController.playNext(track) }

        viewModel.addToQueue(track)
        verify { playerController.addToQueue(listOf(track)) }

        viewModel.toggleFavorite(track.id)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { favoritesRepository.toggleFavorite(track.id) }
    }
}

