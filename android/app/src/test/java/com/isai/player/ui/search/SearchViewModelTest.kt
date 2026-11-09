package com.isai.player.ui.search

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
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var libraryRepository: LibraryRepository
    private lateinit var favoritesRepository: FavoritesRepository
    private lateinit var playerController: PlayerController

    private fun createSampleTrack(id: Long, title: String, artist: String, album: String): Track = Track(
        id = id,
        uri = "uri:$id",
        title = title,
        titleSort = title.lowercase(),
        artist = artist,
        albumArtist = artist,
        album = album,
        year = 2024,
        trackNo = 1,
        discNo = 1,
        durationMs = 210000L,
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
        favoritesRepository = mockk(relaxed = true)
        playerController = mockk(relaxed = true)

        val results = listOf(
            createSampleTrack(1L, "Chinna Chinna Aasai", "A. R. Rahman", "Roja"),
            createSampleTrack(2L, "Pudhu Vellai Mazhai", "A. R. Rahman", "Roja")
        )

        every { libraryRepository.searchTracks(any()) } returns flowOf(results)
        every { favoritesRepository.observeFavoriteTrackIds() } returns flowOf(setOf(1L))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialQueryIsEmptyAndNoResults() = runTest {
        val viewModel = SearchViewModel(libraryRepository, playerController, favoritesRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("", viewModel.searchQuery.value)
        assertTrue(viewModel.uiState.value.matchingTracks.isEmpty())
    }

    @Test
    fun queryDebouncesAndEmitsCategorizedResults() = runTest {
        val viewModel = SearchViewModel(libraryRepository, playerController, favoritesRepository)

        viewModel.onQueryChange("Roja")
        // Before 300ms debounce
        advanceTimeBy(100L)
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.uiState.value.matchingTracks.isEmpty())

        // After 300ms debounce
        advanceTimeBy(250L)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.matchingTracks.size)
        assertEquals(1, state.matchingAlbums.size)
        assertEquals("Roja", state.matchingAlbums[0].title)
        assertEquals(1, state.matchingArtists.size)
        assertEquals("A. R. Rahman", state.matchingArtists[0].name)
    }

    @Test
    fun clearQueryResetsState() = runTest {
        val viewModel = SearchViewModel(libraryRepository, playerController, favoritesRepository)

        viewModel.onQueryChange("Roja")
        advanceTimeBy(350L)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.clearQuery()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("", viewModel.searchQuery.value)
    }

    @Test
    fun playAndMenuActionsDelegate() = runTest {
        val viewModel = SearchViewModel(libraryRepository, playerController, favoritesRepository)
        viewModel.onQueryChange("Roja")
        advanceTimeBy(350L)
        testDispatcher.scheduler.advanceUntilIdle()

        val track = viewModel.uiState.value.matchingTracks.first()
        viewModel.playTrack(track)
        verify { playerController.playTrack(track, any(), 0) }

        viewModel.playNext(track)
        verify { playerController.playNext(track) }

        viewModel.addToQueue(track)
        verify { playerController.addToQueue(listOf(track)) }

        viewModel.toggleFavorite(track.id)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { favoritesRepository.toggleFavorite(track.id) }
    }
}

