package com.isai.player.data.equalizer

import kotlin.math.max
import kotlin.math.pow

data class ClippingAnalysis(
    val maxBoostDb: Float,
    val isClippingPotential: Boolean,
    val attenuationDb: Float,
    val linearScaleFactor: Float,
    val effectivePreampDb: Float
)

object ClippingMath {

    /**
     * Evaluates maximum headroom and calculates auto-compensation attenuation
     * to eliminate digital clipping when positive EQ or Preamp boosts are present.
     */
    fun analyze(
        preampDb: Float,
        bandGainsMilliBels: List<Int>,
        preventClipping: Boolean
    ): ClippingAnalysis {
        val maxBandGainDb = if (bandGainsMilliBels.isEmpty()) 0f else {
            max(0f, bandGainsMilliBels.maxOrNull()?.let { it / 100f } ?: 0f)
        }
        val positivePreampDb = max(0f, preampDb)
        val maxBoostDb = positivePreampDb + maxBandGainDb

        val isClippingPotential = maxBoostDb > 0.1f

        return if (preventClipping && isClippingPotential) {
            val attenuationDb = -maxBoostDb
            val linearScale = 10f.pow(attenuationDb / 20f)
            val effectivePreamp = preampDb + attenuationDb
            ClippingAnalysis(
                maxBoostDb = maxBoostDb,
                isClippingPotential = true,
                attenuationDb = attenuationDb,
                linearScaleFactor = linearScale,
                effectivePreampDb = effectivePreamp
            )
        } else {
            ClippingAnalysis(
                maxBoostDb = maxBoostDb,
                isClippingPotential = isClippingPotential,
                attenuationDb = 0f,
                linearScaleFactor = 1.0f,
                effectivePreampDb = preampDb
            )
        }
    }
}

