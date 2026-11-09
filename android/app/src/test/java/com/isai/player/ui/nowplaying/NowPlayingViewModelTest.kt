package com.isai.player.ui.nowplaying

import com.isai.player.data.model.Track
import com.isai.player.data.repository.FavoritesRepository
import com.isai.player.data.repository.LiftBehavior
import com.isai.player.data.repository.LyricsRepository
import com.isai.player.data.repository.SeekBarStyle
import com.isai.player.data.repository.SettingsRepository
import com.isai.player.data.repository.WaveformRepository
import com.isai.player.data.repository.WaveformState
import com.isai.player.playback.PlayerController
import com.isai.player.playback.PlayerState
import com.isai.player.ui.components.SwipeDirection
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NowPlayingViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var playerController: PlayerController
    private lateinit var favoritesRepository: FavoritesRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var waveformRepository: WaveformRepository
    private lateinit var lyricsRepository: LyricsRepository

    private val playerStateFlow = MutableStateFlow(PlayerState())
    private val liftBehaviorFlow = MutableStateFlow(LiftBehavior.PAUSE)
    private val seekBarStyleFlow = MutableStateFlow(SeekBarStyle.AUTO)

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
        durationMs = 240000L,
        mimeType = "audio/mpeg",
        sizeBytes = 1000L,
        lastModified = 0L,
        artPath = "/art/$id.jpg",
        folderPath = "Music",
        dateAdded = 0L
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        playerController = mockk(relaxed = true)
        favoritesRepository = mockk(relaxed = true)
        settingsRepository = mockk(relaxed = true)
        waveformRepository = mockk(relaxed = true)
        lyricsRepository = mockk(relaxed = true)

        every { playerController.playerState } returns playerStateFlow
        every { settingsRepository.liftBehavior } returns liftBehaviorFlow
        every { settingsRepository.seekBarStyle } returns seekBarStyleFlow
        every { favoritesRepository.observeIsFavorite(any()) } returns flowOf(false)
        every { waveformRepository.observeWaveform(any(), any(), any(), any()) } returns flowOf(WaveformState.Unavailable)
        every { lyricsRepository.observeLyrics(any()) } returns flowOf(null)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun scrubPreviewLifecycle() = runTest {
        val viewModel = NowPlayingViewModel(
            playerController,
            favoritesRepository,
            settingsRepository,
            waveformRepository,
            lyricsRepository
        )

        assertNull(viewModel.scrubPreviewProgress.value)

        viewModel.onScrubPreview(ScrubOwner.BAR, 0.45f)
        assertEquals(0.45f, viewModel.scrubPreviewProgress.value ?: 0f, 0.001f)

        viewModel.onScrubEnd(ScrubOwner.BAR, commit = true, progress = 0.45f)
        assertNull(viewModel.scrubPreviewProgress.value)
        verify { playerController.seekToProgress(0.45f) }
        verify { playerController.play() }
    }

    @Test
    fun liftStopHonorsSetting() = runTest {
        val viewModel = NowPlayingViewModel(
            playerController,
            favoritesRepository,
            settingsRepository,
            waveformRepository,
            lyricsRepository
        )

        // When LiftBehavior is PAUSE
        liftBehaviorFlow.value = LiftBehavior.PAUSE
        viewModel.onLiftedStop()
        verify { playerController.pause() }

        // When LiftBehavior is STOP
        liftBehaviorFlow.value = LiftBehavior.STOP
        viewModel.onLiftedStop()
        verify { playerController.pause() }
        verify { playerController.seekTo(0L) }
    }

    @Test
    fun skipUpdatesLastSwipeDirection() = runTest {
        val viewModel = NowPlayingViewModel(
            playerController,
            favoritesRepository,
            settingsRepository,
            waveformRepository,
            lyricsRepository
        )

        viewModel.skipToNext()
        assertEquals(SwipeDirection.NEXT, viewModel.lastSwipeDirection.value)
        verify { playerController.skipToNext() }

        viewModel.skipToPrevious()
        assertEquals(SwipeDirection.PREVIOUS, viewModel.lastSwipeDirection.value)
        verify { playerController.skipToPrevious() }
    }

    @Test
    fun toggleFavoriteInvokesRepository() = runTest {
        val track = createSampleTrack(id = 42L)
        playerStateFlow.value = PlayerState(currentTrack = track)

        val viewModel = NowPlayingViewModel(
            playerController,
            favoritesRepository,
            settingsRepository,
            waveformRepository,
            lyricsRepository
        )
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.toggleFavorite()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { favoritesRepository.toggleFavorite(42L) }
    }

    @Test
    fun sleepTimerDelegatesToPlayerController() = runTest {
        val viewModel = NowPlayingViewModel(
            playerController,
            favoritesRepository,
            settingsRepository,
            waveformRepository,
            lyricsRepository
        )

        viewModel.startSleepTimer(30 * 60 * 1000L, stopAtEndOfTrack = false)
        verify { playerController.startSleepTimer(30 * 60 * 1000L, false) }

        viewModel.cancelSleepTimer()
        verify { playerController.cancelSleepTimer() }
    }
}
