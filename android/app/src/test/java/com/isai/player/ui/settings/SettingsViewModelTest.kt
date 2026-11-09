package com.isai.player.ui.settings

import android.net.Uri
import com.isai.player.data.repository.LibraryRepository
import com.isai.player.data.repository.LiftBehavior
import com.isai.player.data.repository.LyricsRepository
import com.isai.player.data.repository.LyricsTextSize
import com.isai.player.data.repository.SeekBarStyle
import com.isai.player.data.repository.SettingsRepository
import com.isai.player.data.repository.ThemeMode
import com.isai.player.data.repository.WaveformRepository
import com.isai.player.data.scanner.LibraryScanner
import com.isai.player.data.scanner.ScanState
import com.isai.player.playback.PlayerController
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var libraryRepository: LibraryRepository
    private lateinit var libraryScanner: LibraryScanner
    private lateinit var playerController: PlayerController
    private lateinit var waveformRepository: WaveformRepository
    private lateinit var lyricsRepository: LyricsRepository

    private val themeModeFlow = MutableStateFlow(ThemeMode.SYSTEM)
    private val liftBehaviorFlow = MutableStateFlow(LiftBehavior.PAUSE)
    private val dynamicAccentFlow = MutableStateFlow(true)
    private val seekBarStyleFlow = MutableStateFlow(SeekBarStyle.AUTO)
    private val preGenWaveformsFlow = MutableStateFlow(false)
    private val waveformCountFlow = MutableStateFlow(0)
    private val lyricsTextSizeFlow = MutableStateFlow(LyricsTextSize.MEDIUM)
    private val keepScreenOnLyricsFlow = MutableStateFlow(false)
    private val lyricsCountFlow = MutableStateFlow(0)
    private val folderUriFlow = MutableStateFlow<String?>("content://tree/music")
    private val folderNameFlow = MutableStateFlow<String?>("Music")
    private val scanStateFlow = MutableStateFlow<ScanState>(ScanState.Idle)
    private val timerRemainingFlow = MutableStateFlow(0L)
    private val stopAtEndFlow = MutableStateFlow(false)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        settingsRepository = mockk(relaxed = true)
        libraryRepository = mockk(relaxed = true)
        libraryScanner = mockk(relaxed = true)
        playerController = mockk(relaxed = true)
        waveformRepository = mockk(relaxed = true)
        lyricsRepository = mockk(relaxed = true)

        every { settingsRepository.themeMode } returns themeModeFlow
        every { settingsRepository.liftBehavior } returns liftBehaviorFlow
        every { settingsRepository.dynamicAccentColor } returns dynamicAccentFlow
        every { settingsRepository.seekBarStyle } returns seekBarStyleFlow
        every { settingsRepository.lyricsTextSize } returns lyricsTextSizeFlow
        every { settingsRepository.keepScreenOnWhileLyrics } returns keepScreenOnLyricsFlow
        every { settingsRepository.preGenerateWaveforms } returns preGenWaveformsFlow
        every { waveformRepository.observeWaveformCount() } returns waveformCountFlow
        every { lyricsRepository.observeLyricsCount() } returns lyricsCountFlow
        every { settingsRepository.selectedFolderUri } returns folderUriFlow
        every { settingsRepository.selectedFolderName } returns folderNameFlow
        every { libraryScanner.scanState } returns scanStateFlow
        every { playerController.sleepTimer.remainingMs } returns timerRemainingFlow
        every { playerController.sleepTimer.isStopAtEndOfTrack } returns stopAtEndFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialUiStateReflectsPreferences() = runTest {
        val viewModel = SettingsViewModel(
            settingsRepository,
            libraryRepository,
            libraryScanner,
            playerController,
            waveformRepository,
            lyricsRepository
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(ThemeMode.SYSTEM, state.themeMode)
        assertEquals(LiftBehavior.PAUSE, state.liftBehavior)
        assertEquals(true, state.dynamicAccentColor)
        assertEquals(SeekBarStyle.AUTO, state.seekBarStyle)
        assertEquals(LyricsTextSize.MEDIUM, state.lyricsTextSize)
        assertEquals("content://tree/music", state.selectedFolderUri)
        assertEquals("Music", state.selectedFolderName)
    }

    @Test
    fun setThemeModeUpdatesRepository() = runTest {
        val viewModel = SettingsViewModel(
            settingsRepository,
            libraryRepository,
            libraryScanner,
            playerController,
            waveformRepository,
            lyricsRepository
        )

        viewModel.setThemeMode(ThemeMode.DARK)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { settingsRepository.setThemeMode(ThemeMode.DARK) }
    }

    @Test
    fun setSeekBarStyleUpdatesRepository() = runTest {
        val viewModel = SettingsViewModel(
            settingsRepository,
            libraryRepository,
            libraryScanner,
            playerController,
            waveformRepository,
            lyricsRepository
        )

        viewModel.setSeekBarStyle(SeekBarStyle.WAVEFORM_BARS)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { settingsRepository.setSeekBarStyle(SeekBarStyle.WAVEFORM_BARS) }
    }

    @Test
    fun setLyricsTextSizeUpdatesRepository() = runTest {
        val viewModel = SettingsViewModel(
            settingsRepository,
            libraryRepository,
            libraryScanner,
            playerController,
            waveformRepository,
            lyricsRepository
        )

        viewModel.setLyricsTextSize(LyricsTextSize.LARGE)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { settingsRepository.setLyricsTextSize(LyricsTextSize.LARGE) }
    }

    @Test
    fun clearWaveformCacheDelegatesToRepository() = runTest {
        val viewModel = SettingsViewModel(
            settingsRepository,
            libraryRepository,
            libraryScanner,
            playerController,
            waveformRepository,
            lyricsRepository
        )

        viewModel.clearWaveformCache()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { waveformRepository.clearWaveformCache() }
    }

    @Test
    fun purgeMissingTracksInvokesLibraryRepository() = runTest {
        coEvery { libraryRepository.purgeMissingTracks() } returns 5

        val viewModel = SettingsViewModel(
            settingsRepository,
            libraryRepository,
            libraryScanner,
            playerController,
            waveformRepository,
            lyricsRepository
        )

        viewModel.purgeMissingTracks()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { libraryRepository.purgeMissingTracks() }
        assertEquals(5, viewModel.uiState.value.purgedCount)
    }
}
