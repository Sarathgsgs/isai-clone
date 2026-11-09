package com.isai.player.data.waveform

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WaveformMathTest {

    @Test
    fun normalizePeaksScalesBy95thPercentileAndGamma() {
        // Create 100 raw peaks: 95 normal peaks around 1000f, and 5 huge spikes at 30000f
        val raw = FloatArray(100) { i ->
            if (i >= 95) 30000f else 1000f
        }

        val normalized = WaveformMath.normalizePeaks(raw, gamma = 0.7f)
        assertEquals(100, normalized.size)

        // The 95 normal samples should be scaled near 1.0 (255) rather than crushed by the 30000 spikes
        val normalSampleByte = normalized[10].toInt() and 0xFF
        assertTrue("Normal samples must not be crushed; got $normalSampleByte", normalSampleByte > 200)

        // The huge spikes should saturate at 255
        val spikeSampleByte = normalized[98].toInt() and 0xFF
        assertEquals(255, spikeSampleByte)
    }

    @Test
    fun normalizePeaksEmptyOrZeroGivesZeros() {
        val empty = WaveformMath.normalizePeaks(FloatArray(0))
        assertEquals(0, empty.size)

        val zeros = WaveformMath.normalizePeaks(FloatArray(20) { 0f })
        assertEquals(20, zeros.size)
        assertTrue(zeros.all { it.toInt() == 0 })
    }

    @Test
    fun resampleBucketsDownsamplingUsesMaxPool() {
        // 200 stored buckets with isolated transient peaks
        val stored = ByteArray(200) { 0 }
        // Place a peak at index 50
        stored[50] = 250.toByte()

        // Downsample to 50 visual bars
        val visualBars = WaveformMath.resampleBuckets(stored, 50)
        assertEquals(50, visualBars.size)

        // Window corresponding to index 50 in 200 items -> bucket ~12 in 50 items
        val barIndex = (50 * 50) / 200
        val peakBar = visualBars[barIndex]
        assertTrue("Max-pooling must capture peak amplitude in bar window; got $peakBar", peakBar > 0.9f)

        // Non-peak bars should remain at floor level (0.12f)
        assertEquals(0.12f, visualBars[0], 0.001f)
    }

    @Test
    fun resampleBucketsUpsamplingInterpolatesSmoothly() {
        val stored = ByteArray(2)
        stored[0] = 0.toByte()
        stored[1] = 255.toByte()

        val upsampled = WaveformMath.resampleBuckets(stored, 5)
        assertEquals(5, upsampled.size)

        // First bar: ~0.12f (clamped floor)
        assertEquals(0.12f, upsampled[0], 0.001f)
        // Middle bars should monotonically increase
        assertTrue(upsampled[1] < upsampled[2])
        assertTrue(upsampled[2] < upsampled[3])
        // Last bar: 1.0f
        assertEquals(1.0f, upsampled[4], 0.001f)
    }

    @Test
    fun deterministicFallbackIsConsistentForSameSeed() {
        val trackId = 987654321L
        val fb1 = WaveformMath.generateDeterministicFallback(trackId, 200)
        val fb2 = WaveformMath.generateDeterministicFallback(trackId, 200)

        assertEquals(200, fb1.size)
        assertEquals(200, fb2.size)
        assertTrue("Fallback must be completely deterministic for same seed", fb1.contentEquals(fb2))

        // Different seeds produce distinct waveforms
        val fbOther = WaveformMath.generateDeterministicFallback(123456789L, 200)
        assertFalse("Different seeds must produce different waveforms", fb1.contentEquals(fbOther))
    }

    private fun assertFalse(message: String, condition: Boolean) {
        org.junit.Assert.assertFalse(message, condition)
    }
}

