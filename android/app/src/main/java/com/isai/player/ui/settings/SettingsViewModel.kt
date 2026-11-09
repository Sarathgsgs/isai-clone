package com.isai.player.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val liftBehavior: LiftBehavior = LiftBehavior.PAUSE,
    val dynamicAccentColor: Boolean = true,
    val seekBarStyle: SeekBarStyle = SeekBarStyle.AUTO,
    val lyricsTextSize: LyricsTextSize = LyricsTextSize.MEDIUM,
    val keepScreenOnWhileLyrics: Boolean = false,
    val selectedFolderUri: String? = null,
    val selectedFolderName: String? = null,
    val isScanning: Boolean = false,
    val scanStatusMessage: String? = null,
    val purgedCount: Int? = null,
    val sleepTimerRemainingMs: Long = 0L,
    val isStopAtEndOfTrack: Boolean = false,
    val waveformCount: Int = 0,
    val preGenerateWaveforms: Boolean = false,
    val lyricsCount: Int = 0
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val libraryRepository: LibraryRepository,
    private val libraryScanner: LibraryScanner,
    private val playerController: PlayerController,
    private val waveformRepository: WaveformRepository,
    private val lyricsRepository: LyricsRepository
) : ViewModel() {

    private val scanMessageFlow = MutableStateFlow<String?>(null)
    private val purgedCountFlow = MutableStateFlow<Int?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.themeMode,
        settingsRepository.liftBehavior,
        settingsRepository.dynamicAccentColor,
        settingsRepository.seekBarStyle,
        settingsRepository.lyricsTextSize,
        settingsRepository.keepScreenOnWhileLyrics,
        settingsRepository.preGenerateWaveforms,
        waveformRepository.observeWaveformCount(),
        lyricsRepository.observeLyricsCount(),
        settingsRepository.selectedFolderUri,
        settingsRepository.selectedFolderName,
        libraryScanner.scanState,
        scanMessageFlow
    ) { args: Array<Any?> ->
        val theme = args[0] as ThemeMode
        val lift = args[1] as LiftBehavior
        val dynamicAccent = args[2] as Boolean
        val seekStyle = args[3] as SeekBarStyle
        val textSize = args[4] as LyricsTextSize
        val keepScreenOn = args[5] as Boolean
        val preGenWaveforms = args[6] as Boolean
        val waveCount = args[7] as Int
        val lyricsCount = args[8] as Int
        val folderUri = args[9] as String?
        val folderName = args[10] as String?
        val scanState = args[11] as ScanState
        val scanMsg = args[12] as String?

        val isScanning = scanState is ScanState.Scanning
        val statusMsg = when (scanState) {
            is ScanState.Completed -> "Scan complete: ${scanState.totalTracks} total tracks"
            is ScanState.Error -> "Scan error: ${scanState.message}"
            else -> scanMsg
        }

        SettingsUiState(
            themeMode = theme,
            liftBehavior = lift,
            dynamicAccentColor = dynamicAccent,
            seekBarStyle = seekStyle,
            lyricsTextSize = textSize,
            keepScreenOnWhileLyrics = keepScreenOn,
            selectedFolderUri = folderUri,
            selectedFolderName = folderName,
            isScanning = isScanning,
            scanStatusMessage = statusMsg,
            purgedCount = purgedCountFlow.value,
            sleepTimerRemainingMs = playerController.sleepTimer.remainingMs.value,
            isStopAtEndOfTrack = playerController.sleepTimer.isStopAtEndOfTrack.value,
            waveformCount = waveCount,
            preGenerateWaveforms = preGenWaveforms,
            lyricsCount = lyricsCount
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState()
    )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.setThemeMode(mode)
        }
    }

    fun setLiftBehavior(behavior: LiftBehavior) {
        viewModelScope.launch {
            settingsRepository.setLiftBehavior(behavior)
        }
    }

    fun setDynamicAccentColor(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDynamicAccentColor(enabled)
        }
    }

    fun setSeekBarStyle(style: SeekBarStyle) {
        viewModelScope.launch {
            settingsRepository.setSeekBarStyle(style)
        }
    }

    fun setLyricsTextSize(size: LyricsTextSize) {
        viewModelScope.launch {
            settingsRepository.setLyricsTextSize(size)
        }
    }

    fun setKeepScreenOnWhileLyrics(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setKeepScreenOnWhileLyrics(enabled)
        }
    }

    fun setPreGenerateWaveforms(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setPreGenerateWaveforms(enabled)
        }
    }

    fun clearWaveformCache() {
        viewModelScope.launch {
            waveformRepository.clearWaveformCache()
        }
    }

    fun rescanLibrary() {
        val uriString = uiState.value.selectedFolderUri ?: return
        val uri = Uri.parse(uriString)
        viewModelScope.launch {
            libraryScanner.startScan(uri)
        }
    }

    fun updateFolderAndRescan(uri: Uri, folderName: String) {
        viewModelScope.launch {
            settingsRepository.setSelectedFolder(uri.toString(), folderName)
            libraryScanner.startScan(uri)
        }
    }

    fun purgeMissingTracks() {
        viewModelScope.launch {
            val purged = libraryRepository.purgeMissingTracks()
            purgedCountFlow.value = purged
        }
    }

    fun startSleepTimer(durationMs: Long, stopAtEndOfTrack: Boolean) {
        playerController.startSleepTimer(durationMs, stopAtEndOfTrack)
    }

    fun cancelSleepTimer() {
        playerController.cancelSleepTimer()
    }

    fun clearStatusMessage() {
        scanMessageFlow.value = null
        purgedCountFlow.value = null
        libraryScanner.resetState()
    }
}
