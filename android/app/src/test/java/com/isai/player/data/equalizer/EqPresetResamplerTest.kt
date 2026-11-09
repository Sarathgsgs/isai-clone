package com.isai.player.data.equalizer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EqPresetResamplerTest {

    @Test
    fun resample_flatPreset_returnsAllZerosOnAnyTargetBandCount() {
        val flat = EqPresets.BUILT_IN_PRESETS.first { it.name == "Flat" }

        val target5 = listOf(60, 230, 910, 3600, 14000)
        val resampled5 = EqPresetResampler.resample(flat, target5)
        assertEquals(5, resampled5.size)
        assertTrue(resampled5.all { it == 0 })

        val target10 = listOf(31, 62, 125, 250, 500, 1000, 2000, 4000, 8000, 16000)
        val resampled10 = EqPresetResampler.resample(flat, target10)
        assertEquals(10, resampled10.size)
        assertTrue(resampled10.all { it == 0 })
    }

    @Test
    fun resample_bassBoostPreset_preservesBassDominanceAcrossBands() {
        val bassBoost = EqPresets.BUILT_IN_PRESETS.first { it.name == "Bass Boost" }
        val target5 = listOf(60, 230, 910, 3600, 14000)

        val resampled = EqPresetResampler.resample(bassBoost, target5)
        assertEquals(5, resampled.size)

        // Lowest band (60 Hz) should have higher gain than upper bands (3600 & 14000 Hz)
        assertTrue("60 Hz gain should be > 3600 Hz gain", resampled[0] > resampled[3])
        assertTrue("60 Hz gain should be > 14000 Hz gain", resampled[0] > resampled[4])
    }

    @Test
    fun resample_clampsToHardwareLimits() {
        val extremePreset = EqPreset(
            name = "Extreme",
            referenceFreqsHz = listOf(100, 1000, 10000),
            referenceGainsDb = listOf(30.0f, 0.0f, -30.0f)
        )
        val target = listOf(100, 1000, 10000)
        val resampled = EqPresetResampler.resample(
            extremePreset,
            target,
            minLevelMilliBels = -1200,
            maxLevelMilliBels = 1200
        )

        assertEquals(1200, resampled[0])
        assertEquals(0, resampled[1])
        assertEquals(-1200, resampled[2])
    }

    @Test
    fun resample_handlesFrequenciesOutsideReferenceRange() {
        val reference = EqPreset(
            name = "MidOnly",
            referenceFreqsHz = listOf(200, 2000),
            referenceGainsDb = listOf(4.0f, 8.0f)
        )
        // 50 Hz is below 200 Hz; 10000 Hz is above 2000 Hz
        val target = listOf(50, 200, 2000, 10000)
        val resampled = EqPresetResampler.resample(reference, target)

        assertEquals(400, resampled[0]) // clamped to first (4.0 dB = 400 mB)
        assertEquals(400, resampled[1])
        assertEquals(800, resampled[2])
        assertEquals(800, resampled[3]) // clamped to last (8.0 dB = 800 mB)
    }
}

