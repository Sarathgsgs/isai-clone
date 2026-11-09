package com.isai.player.data.waveform

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sin

/**
 * Pure mathematical algorithms for waveform peak normalization, gamma correction,
 * bar resampling (max-pooling and linear interpolation), and deterministic calm fallbacks.
 */
object WaveformMath {

    const val DEFAULT_BUCKET_COUNT = 200

    /**
     * Normalizes raw peak amplitudes (0..32767 for 16-bit PCM) into unsigned bytes (0..255).
     *
     * Scaling uses the 95th percentile (P95) rather than absolute maximum to prevent
     * isolated transient spikes/clicks from compressing the dynamic visual range of the track.
     * Mild gamma correction (default 0.7) expands quiet passages so subtle dynamics remain visible.
     */
    fun normalizePeaks(rawPeaks: FloatArray, gamma: Float = 0.7f): ByteArray {
        if (rawPeaks.isEmpty()) return ByteArray(0)

        // Extract positive peaks to find 95th percentile
        val nonZero = rawPeaks.filter { it > 0f }
        if (nonZero.isEmpty()) return ByteArray(rawPeaks.size) { 0 }

        val sorted = nonZero.sorted()
        val p95Index = ((sorted.size - 1) * 0.95f).toInt().coerceIn(0, sorted.size - 1)
        val p95 = sorted[p95Index].coerceAtLeast(1e-4f)

        return ByteArray(rawPeaks.size) { i ->
            val ratio = (rawPeaks[i] / p95).coerceIn(0f, 1f)
            val gammaCorrected = ratio.pow(gamma)
            (gammaCorrected * 255f).toInt().coerceIn(0, 255).toByte()
        }
    }

    /**
     * Resamples the stored bucket array (typically 200 values, 0..255) to match the visual bar count
     * calculated from screen width.
     *
     * - Downsampling (storedCount > targetCount): Uses Max-Pooling within each window to preserve
     *   sharp drum hits and musical transients.
     * - Upsampling (storedCount <= targetCount): Uses linear interpolation between neighboring buckets.
     *
     * Returns an array of normalized floats in range 0.0f..1.0f.
     */
    fun resampleBuckets(storedBuckets: ByteArray, targetCount: Int): FloatArray {
        if (targetCount <= 0) return FloatArray(0)
        if (storedBuckets.isEmpty()) return FloatArray(targetCount) { 0.12f } // Minimum bar height floor

        val sourceCount = storedBuckets.size
        val result = FloatArray(targetCount)

        if (sourceCount >= targetCount) {
            // Max-pooling downsampling
            for (i in 0 until targetCount) {
                val startIdx = (i.toFloat() * sourceCount / targetCount).toInt().coerceIn(0, sourceCount - 1)
                val endIdx = ((i + 1).toFloat() * sourceCount / targetCount).toInt().coerceIn(startIdx + 1, sourceCount)

                var maxVal = 0
                for (j in startIdx until endIdx) {
                    val v = storedBuckets[j].toInt() and 0xFF
                    if (v > maxVal) maxVal = v
                }
                // Floor at 0.12f so bars are never invisible
                result[i] = (maxVal / 255f).coerceIn(0.12f, 1.0f)
            }
        } else {
            // Linear interpolation upsampling
            for (i in 0 until targetCount) {
                val srcPos = i.toFloat() * (sourceCount - 1) / (targetCount - 1).coerceAtLeast(1)
                val lowIdx = srcPos.toInt().coerceIn(0, sourceCount - 1)
                val highIdx = (lowIdx + 1).coerceIn(0, sourceCount - 1)
                val fraction = srcPos - lowIdx

                val lowVal = (storedBuckets[lowIdx].toInt() and 0xFF) / 255f
                val highVal = (storedBuckets[highIdx].toInt() and 0xFF) / 255f
                val interpolated = lowVal + (highVal - lowVal) * fraction

                result[i] = interpolated.coerceIn(0.12f, 1.0f)
            }
        }

        return result
    }

    /**
     * Generates a deterministic, calm pseudo-waveform seeded uniquely from the track ID.
     * Used as a graceful fallback while audio is decoding or if format is unsupported.
     * Produces smooth, undulating sinusoidal curves with harmonic variation.
     */
    fun generateDeterministicFallback(seedId: Long, bucketCount: Int = DEFAULT_BUCKET_COUNT): ByteArray {
        val bytes = ByteArray(bucketCount)
        val seed = abs(seedId)
        val phase1 = (seed % 100) * 0.05f
        val phase2 = ((seed / 100) % 100) * 0.08f
        val freq1 = 2.0f + (seed % 3)
        val freq2 = 4.0f + ((seed / 3) % 4)

        for (i in 0 until bucketCount) {
            val t = i.toFloat() / bucketCount
            // Superposition of two smooth sinusoidal waves + gentle baseline
            val wave1 = (sin(2 * PI * freq1 * t + phase1).toFloat() + 1f) * 0.5f
            val wave2 = (sin(2 * PI * freq2 * t + phase2).toFloat() + 1f) * 0.5f
            val envelope = (sin(PI * t).toFloat() * 0.4f + 0.6f) // Softer at track start & finish

            val combined = (wave1 * 0.6f + wave2 * 0.4f) * envelope
            val clamped = (combined * 180f + 30f).toInt().coerceIn(20, 220)
            bytes[i] = clamped.toByte()
        }
        return bytes
    }
}

