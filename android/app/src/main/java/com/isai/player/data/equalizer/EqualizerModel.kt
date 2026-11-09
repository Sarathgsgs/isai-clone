package com.isai.player.data.equalizer

import kotlinx.serialization.Serializable

@Serializable
data class EqualizerBand(
    val index: Int,
    val centerFreqHz: Int,
    val gainMilliBels: Int
)

@Serializable
data class EffectCapabilities(
    val isEqualizerSupported: Boolean = true,
    val bandCount: Int = 5,
    val minLevelMilliBels: Int = -1500,
    val maxLevelMilliBels: Int = 1500,
    val centerFreqsHz: List<Int> = listOf(60, 230, 910, 3600, 14000),
    val isBassBoostSupported: Boolean = true,
    val isVirtualizerSupported: Boolean = true,
    val isLoudnessEnhancerSupported: Boolean = true
) {
    val minGainDb: Float get() = minLevelMilliBels / 100f
    val maxGainDb: Float get() = maxLevelMilliBels / 100f
}

@Serializable
data class EqualizerState(
    val enabled: Boolean = false,
    val presetName: String = "Flat",
    val bandGains: List<Int> = emptyList(), // in milliBels (100 mB = 1 dB)
    val bassBoostStrength: Int = 0,        // 0 to 1000
    val virtualizerStrength: Int = 0,      // 0 to 1000
    val preampDb: Float = 0.0f,            // -12.0f to +12.0f
    val preventClipping: Boolean = true
)

@Serializable
data class EqPreset(
    val name: String,
    val referenceFreqsHz: List<Int> = EqPresets.REFERENCE_FREQUENCIES,
    val referenceGainsDb: List<Float>,
    val isCustom: Boolean = false
)

object EqPresets {
    val REFERENCE_FREQUENCIES = listOf(31, 62, 125, 250, 500, 1000, 2000, 4000, 8000, 16000)

    val BUILT_IN_PRESETS: List<EqPreset> = listOf(
        EqPreset("Flat", referenceGainsDb = listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)),
        EqPreset("Bass Boost", referenceGainsDb = listOf(6.0f, 5.0f, 3.5f, 1.5f, 0f, 0f, 0f, 0f, 0f, 0f)),
        EqPreset("Bass Reducer", referenceGainsDb = listOf(-6.0f, -5.0f, -3.5f, -1.5f, 0f, 0f, 0f, 0f, 0f, 0f)),
        EqPreset("Treble Boost", referenceGainsDb = listOf(0f, 0f, 0f, 0f, 0f, 1.0f, 2.5f, 4.0f, 5.5f, 6.0f)),
        EqPreset("Vocal", referenceGainsDb = listOf(-2.0f, -1.0f, 1.0f, 3.5f, 4.0f, 3.5f, 2.0f, 0.5f, -1.0f, -2.0f)),
        EqPreset("Rock", referenceGainsDb = listOf(4.5f, 3.0f, 1.0f, -0.5f, -1.5f, -0.5f, 1.5f, 3.0f, 4.0f, 4.5f)),
        EqPreset("Pop", referenceGainsDb = listOf(-1.5f, 1.0f, 2.5f, 3.5f, 3.0f, 1.5f, 0f, 1.5f, 2.5f, 3.0f)),
        EqPreset("Jazz", referenceGainsDb = listOf(3.5f, 2.5f, 1.0f, 1.5f, -1.5f, -1.5f, 0f, 1.5f, 3.0f, 3.5f)),
        EqPreset("Classical", referenceGainsDb = listOf(4.0f, 3.0f, 2.0f, 1.5f, -1.0f, -1.0f, 0f, 2.0f, 3.0f, 3.5f)),
        EqPreset("Electronic", referenceGainsDb = listOf(4.5f, 4.0f, 1.5f, 0f, -1.5f, 1.5f, 1.0f, 2.0f, 4.0f, 4.5f)),
        EqPreset("Hip-Hop", referenceGainsDb = listOf(5.5f, 4.5f, 1.5f, 2.0f, -1.0f, -1.0f, 1.0f, -0.5f, 2.0f, 3.0f)),
        EqPreset("Loud", referenceGainsDb = listOf(6.0f, 4.0f, 0f, -1.5f, -2.0f, -1.0f, 0f, -1.5f, 4.5f, 5.5f)),
        EqPreset("Acoustic", referenceGainsDb = listOf(3.5f, 3.0f, 1.5f, 1.0f, 1.5f, 1.5f, 2.5f, 3.0f, 2.5f, 1.5f)),
        EqPreset("Late Night", referenceGainsDb = listOf(3.0f, 2.0f, 0f, -1.0f, -1.0f, -1.0f, -1.5f, -2.0f, -3.0f, -4.0f))
    )
}

