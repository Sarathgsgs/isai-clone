package com.isai.player.ui.nowplaying

import com.isai.player.data.repository.FavoritesRepository
import com.isai.player.data.repository.LyricsRepository
import com.isai.player.data.repository.SettingsRepository
import com.isai.player.data.repository.WaveformRepository
import com.isai.player.playback.PlayerController
import com.isai.player.playback.PlayerState
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScrubOwnershipTest {

    private val testDispatcher = StandardTestDispatcher()
    private val playerController: PlayerController = mockk(relaxed = true)
    private val favoritesRepository: FavoritesRepository = mockk(relaxed = true)
    private val settingsRepository: SettingsRepository = mockk(relaxed = true)
    private val waveformRepository: WaveformRepository = mockk(relaxed = true)
    private val lyricsRepository: LyricsRepository = mockk(relaxed = true)

    private val playerStateFlow = MutableStateFlow(PlayerState())
    private lateinit var viewModel: NowPlayingViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { playerController.playerState } returns playerStateFlow
        viewModel = NowPlayingViewModel(
            playerController = playerController,
            favoritesRepository = favoritesRepository,
            settingsRepository = settingsRepository,
            waveformRepository = waveformRepository,
            lyricsRepository = lyricsRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun barScrubbingUpdatesPreviewAndCommitsSeek() {
        // BAR starts scrubbing
        viewModel.onScrubStart(ScrubOwner.BAR)
        assertEquals(ScrubOwner.BAR, viewModel.scrubOwner.value)

        // BAR previews at 42%
        viewModel.onScrubPreview(ScrubOwner.BAR, 0.42f)
        assertEquals(0.42f, viewModel.scrubPreviewProgress.value ?: 0f, 0.001f)

        // BAR finishes and commits
        viewModel.onScrubEnd(ScrubOwner.BAR, commit = true, progress = 0.42f)
        assertEquals(ScrubOwner.NONE, viewModel.scrubOwner.value)
        assertNull(viewModel.scrubPreviewProgress.value)

        verify(exactly = 1) { playerController.seekToProgress(0.42f) }
        verify(exactly = 1) { playerController.play() }
    }

    @Test
    fun armScrubbingPreventsBarFromInterferingUntilReleased() {
        // ARM starts scrubbing
        viewModel.onScrubStart(ScrubOwner.ARM)
        assertEquals(ScrubOwner.ARM, viewModel.scrubOwner.value)

        // ARM moves to 25%
        viewModel.onScrubPreview(ScrubOwner.ARM, 0.25f)
        assertEquals(0.25f, viewModel.scrubPreviewProgress.value ?: 0f, 0.001f)

        // BAR attempts to scrub while ARM owns it -> should be ignored
        viewModel.onScrubPreview(ScrubOwner.BAR, 0.80f)
        assertEquals(0.25f, viewModel.scrubPreviewProgress.value ?: 0f, 0.001f) // Still 0.25f!

        viewModel.onScrubEnd(ScrubOwner.BAR, commit = true, progress = 0.80f)
        assertEquals(ScrubOwner.ARM, viewModel.scrubOwner.value) // ARM still owns!
        verify(exactly = 0) { playerController.seekToProgress(0.80f) }

        // ARM commits at 25%
        viewModel.onScrubEnd(ScrubOwner.ARM, commit = true, progress = 0.25f)
        assertEquals(ScrubOwner.NONE, viewModel.scrubOwner.value)
        assertNull(viewModel.scrubPreviewProgress.value)
        verify(exactly = 1) { playerController.seekToProgress(0.25f) }
    }

    @Test
    fun liftNeedleResetsScrubOwnerAndCancelsPreview() {
        viewModel.onScrubStart(ScrubOwner.ARM)
        viewModel.onScrubPreview(ScrubOwner.ARM, 0.60f)
        assertEquals(0.60f, viewModel.scrubPreviewProgress.value ?: 0f, 0.001f)

        viewModel.onLiftedStop()
        assertEquals(ScrubOwner.NONE, viewModel.scrubOwner.value)
        assertNull(viewModel.scrubPreviewProgress.value)
    }
}
