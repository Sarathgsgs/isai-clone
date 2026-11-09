package com.isai.player.data.equalizer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClippingCompensationTest {

    @Test
    fun analyze_flatNoPreamp_reportsNoClippingPotential() {
        val analysis = ClippingMath.analyze(
            preampDb = 0f,
            bandGainsMilliBels = listOf(0, 0, 0, 0, 0),
            preventClipping = true
        )
        assertFalse(analysis.isClippingPotential)
        assertEquals(0f, analysis.maxBoostDb, 0.01f)
        assertEquals(0f, analysis.attenuationDb, 0.01f)
        assertEquals(1.0f, analysis.linearScaleFactor, 0.01f)
        assertEquals(0f, analysis.effectivePreampDb, 0.01f)
    }

    @Test
    fun analyze_positiveBoostWithPreventClipping_appliesExactAttenuation() {
        val analysis = ClippingMath.analyze(
            preampDb = 2.0f,
            bandGainsMilliBels = listOf(400, 200, 0, -200, 100), // Max is +4.0 dB
            preventClipping = true
        )
        // Max boost = 2.0 + 4.0 = 6.0 dB
        assertTrue(analysis.isClippingPotential)
        assertEquals(6.0f, analysis.maxBoostDb, 0.01f)
        assertEquals(-6.0f, analysis.attenuationDb, 0.01f)
        assertEquals(-4.0f, analysis.effectivePreampDb, 0.01f) // 2.0 - 6.0 = -4.0 dB
        assertTrue("Scale factor should be < 1.0 (approx 0.501)", analysis.linearScaleFactor in 0.49f..0.52f)
    }

    @Test
    fun analyze_positiveBoostWithoutPreventClipping_doesNotAttenuate() {
        val analysis = ClippingMath.analyze(
            preampDb = 3.0f,
            bandGainsMilliBels = listOf(300, 0, 0),
            preventClipping = false
        )
        assertTrue(analysis.isClippingPotential)
        assertEquals(6.0f, analysis.maxBoostDb, 0.01f)
        assertEquals(0f, analysis.attenuationDb, 0.01f)
        assertEquals(1.0f, analysis.linearScaleFactor, 0.01f)
        assertEquals(3.0f, analysis.effectivePreampDb, 0.01f)
    }

    @Test
    fun analyze_negativeGainsOnly_noClippingTriggered() {
        val analysis = ClippingMath.analyze(
            preampDb = -3.0f,
            bandGainsMilliBels = listOf(-100, -200, -500),
            preventClipping = true
        )
        assertFalse(analysis.isClippingPotential)
        assertEquals(0f, analysis.maxBoostDb, 0.01f)
        assertEquals(0f, analysis.attenuationDb, 0.01f)
    }
}

