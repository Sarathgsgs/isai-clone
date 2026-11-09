package com.isai.player.data.repository

import kotlinx.coroutines.flow.Flow

enum class ThemeMode {
    SYSTEM,
    DARK,
    LIGHT
}

enum class LiftBehavior {
    PAUSE,
    STOP
}

enum class SeekBarStyle {
    AUTO,
    WAVEFORM_BARS,
    WAVEFORM_LINE,
    CLASSIC
}

enum class LyricsTextSize {
    SMALL,
    MEDIUM,
    LARGE
}

interface SettingsRepository {
    val themeMode: Flow<ThemeMode>
    val liftBehavior: Flow<LiftBehavior>
    val selectedFolderUri: Flow<String?>
    val selectedFolderName: Flow<String?>
    val dynamicAccentColor: Flow<Boolean>
    val sleepTimerEndTimestamp: Flow<Long>
    val sleepTimerStopAtEnd: Flow<Boolean>
    val seekBarStyle: Flow<SeekBarStyle>
    val preGenerateWaveforms: Flow<Boolean>
    val lyricsTextSize: Flow<LyricsTextSize>
    val keepScreenOnWhileLyrics: Flow<Boolean>

    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setLiftBehavior(behavior: LiftBehavior)
    suspend fun setSelectedFolder(uri: String, name: String)
    suspend fun clearSelectedFolder()
    suspend fun setDynamicAccentColor(enabled: Boolean)
    suspend fun setSleepTimer(endEpochMs: Long, stopAtEndOfTrack: Boolean)
    suspend fun clearSleepTimer()
    suspend fun setSeekBarStyle(style: SeekBarStyle)
    suspend fun setPreGenerateWaveforms(enabled: Boolean)
    suspend fun setLyricsTextSize(size: LyricsTextSize)
    suspend fun setKeepScreenOnWhileLyrics(enabled: Boolean)
}
