package com.isai.player.data.repository

import com.isai.player.data.equalizer.EqPreset
import com.isai.player.data.equalizer.EqualizerState
import kotlinx.coroutines.flow.Flow

interface EqualizerRepository {
    val equalizerState: Flow<EqualizerState>
    val customPresets: Flow<List<EqPreset>>

    suspend fun setEnabled(enabled: Boolean)
    suspend fun setPreset(name: String, bandGainsMilliBels: List<Int>)
    suspend fun setBandGain(bandIndex: Int, gainMilliBels: Int)
    suspend fun setBassBoost(strength: Int)
    suspend fun setVirtualizer(strength: Int)
    suspend fun setPreamp(preampDb: Float)
    suspend fun setPreventClipping(prevent: Boolean)
    suspend fun saveCustomPreset(preset: EqPreset)
    suspend fun deleteCustomPreset(presetName: String)
}

