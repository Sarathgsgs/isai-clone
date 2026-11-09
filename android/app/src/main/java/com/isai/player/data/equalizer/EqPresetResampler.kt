package com.isai.player.data.equalizer

import kotlin.math.log10
import kotlin.math.roundToInt

object EqPresetResampler {

    /**
     * Resamples a reference preset defined at arbitrary frequencies to target device hardware frequencies
     * using logarithmic frequency interpolation.
     *
     * @param preset The reference preset containing reference frequencies and dB gains.
     * @param targetFreqsHz The target hardware band center frequencies in Hertz (e.g. [60, 230, 910, 3600, 14000]).
     * @param minLevelMilliBels Minimum hardware band level (typically -1500 mB = -15 dB).
     * @param maxLevelMilliBels Maximum hardware band level (typically +1500 mB = +15 dB).
     * @return List of gain values in milliBels for each target frequency, clamped to hardware limits.
     */
    fun resample(
        preset: EqPreset,
        targetFreqsHz: List<Int>,
        minLevelMilliBels: Int = -1500,
        maxLevelMilliBels: Int = 1500
    ): List<Int> {
        if (targetFreqsHz.isEmpty()) return emptyList()
        val refFreqs = preset.referenceFreqsHz
        val refGains = preset.referenceGainsDb

        if (refFreqs.isEmpty() || refGains.isEmpty() || refFreqs.size != refGains.size) {
            return targetFreqsHz.map { 0 }
        }

        return targetFreqsHz.map { freq ->
            val gainDb = interpolateLogFreq(freq, refFreqs, refGains)
            val milliBels = (gainDb * 100f).roundToInt()
            milliBels.coerceIn(minLevelMilliBels, maxLevelMilliBels)
        }
    }

    private fun interpolateLogFreq(
        freq: Int,
        refFreqs: List<Int>,
        refGains: List<Float>
    ): Float {
        val f = freq.coerceAtLeast(1)
        val firstFreq = refFreqs.first().coerceAtLeast(1)
        val lastFreq = refFreqs.last().coerceAtLeast(1)

        if (f <= firstFreq) return refGains.first()
        if (f >= lastFreq) return refGains.last()

        val logF = log10(f.toDouble())

        // Find surrounding interval
        for (i in 0 until refFreqs.size - 1) {
            val f0 = refFreqs[i].coerceAtLeast(1)
            val f1 = refFreqs[i + 1].coerceAtLeast(1)
            if (f in f0..f1) {
                val logF0 = log10(f0.toDouble())
                val logF1 = log10(f1.toDouble())
                val span = logF1 - logF0
                if (span <= 1e-6) return refGains[i]

                val t = ((logF - logF0) / span).toFloat()
                return refGains[i] + t * (refGains[i + 1] - refGains[i])
            }
        }

        return refGains.last()
    }
}

