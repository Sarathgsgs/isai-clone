package com.isai.player.ui.equalizer

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.isai.player.data.equalizer.ClippingAnalysis
import com.isai.player.data.equalizer.ClippingMath
import com.isai.player.data.equalizer.EffectCapabilities
import com.isai.player.data.equalizer.EqPreset
import com.isai.player.data.equalizer.EqPresetResampler
import com.isai.player.data.equalizer.EqPresets
import com.isai.player.data.equalizer.EqualizerState
import com.isai.player.data.repository.EqualizerRepository
import com.isai.player.playback.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EqualizerUiState(
    val state: EqualizerState = EqualizerState(),
    val capabilities: EffectCapabilities = EffectCapabilities(),
    val customPresets: List<EqPreset> = emptyList(),
    val clippingAnalysis: ClippingAnalysis = ClippingAnalysis(0f, false, 0f, 1f, 0f)
)

@HiltViewModel
class EqualizerViewModel @Inject constructor(
    private val equalizerRepository: EqualizerRepository,
    private val playerController: PlayerController
) : ViewModel() {

    val uiState: StateFlow<EqualizerUiState> = combine(
        equalizerRepository.equalizerState,
        equalizerRepository.customPresets,
        playerController.effectCapabilities
    ) { state, customPresets, caps ->
        // Ensure band gains match hardware band count
        val bandGains = if (state.bandGains.size == caps.bandCount) {
            state.bandGains
        } else {
            List(caps.bandCount) { 0 }
        }

        val effectiveState = state.copy(bandGains = bandGains)
        val clipping = ClippingMath.analyze(
            preampDb = effectiveState.preampDb,
            bandGainsMilliBels = effectiveState.bandGains,
            preventClipping = effectiveState.preventClipping
        )

        EqualizerUiState(
            state = effectiveState,
            capabilities = caps,
            customPresets = customPresets,
            clippingAnalysis = clipping
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = EqualizerUiState()
    )

    fun toggleEnabled(enabled: Boolean) {
        viewModelScope.launch {
            equalizerRepository.setEnabled(enabled)
        }
    }

    fun selectPreset(preset: EqPreset) {
        val caps = uiState.value.capabilities
        val resampledGains = EqPresetResampler.resample(
            preset = preset,
            targetFreqsHz = caps.centerFreqsHz,
            minLevelMilliBels = caps.minLevelMilliBels,
            maxLevelMilliBels = caps.maxLevelMilliBels
        )
        viewModelScope.launch {
            equalizerRepository.setPreset(preset.name, resampledGains)
        }
    }

    fun updateBandGain(bandIndex: Int, gainMilliBels: Int) {
        viewModelScope.launch {
            equalizerRepository.setBandGain(bandIndex, gainMilliBels)
        }
    }

    fun updateBassBoost(strength: Int) {
        viewModelScope.launch {
            equalizerRepository.setBassBoost(strength)
        }
    }

    fun updateVirtualizer(strength: Int) {
        viewModelScope.launch {
            equalizerRepository.setVirtualizer(strength)
        }
    }

    fun updatePreamp(preampDb: Float) {
        viewModelScope.launch {
            equalizerRepository.setPreamp(preampDb)
        }
    }

    fun togglePreventClipping(prevent: Boolean) {
        viewModelScope.launch {
            equalizerRepository.setPreventClipping(prevent)
        }
    }

    fun saveCustomPreset(name: String) {
        val currentUi = uiState.value
        val freqs = currentUi.capabilities.centerFreqsHz
        val gainsDb = currentUi.state.bandGains.map { it / 100f }
        val preset = EqPreset(
            name = name,
            referenceFreqsHz = freqs,
            referenceGainsDb = gainsDb,
            isCustom = true
        )
        viewModelScope.launch {
            equalizerRepository.saveCustomPreset(preset)
            equalizerRepository.setPreset(name, currentUi.state.bandGains)
        }
    }

    fun deleteCustomPreset(name: String) {
        viewModelScope.launch {
            equalizerRepository.deleteCustomPreset(name)
        }
    }

    fun launchSystemEqualizer(context: Context): Boolean {
        return playerController.launchSystemEqualizer(context)
    }
}

