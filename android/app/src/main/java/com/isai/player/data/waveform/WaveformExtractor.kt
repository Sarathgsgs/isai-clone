package com.isai.player.data.waveform

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.coroutines.coroutineContext
import kotlin.math.abs

/**
 * On-device audio waveform peak extractor using Android MediaExtractor and MediaCodec.
 * Decodes compressed audio (MP3, AAC, FLAC, OGG, WAV) into 16-bit PCM, downmixes to mono,
 * and extracts peak amplitudes for time-based buckets.
 */
class WaveformExtractor(
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default.limitedParallelism(1)
) {

    companion object {
        private const val TIMEOUT_US = 5000L
        private const val LONG_AUDIO_THRESHOLD_US = 12 * 60 * 1_000_000L // 12 minutes
        private const val WINDOW_DECODE_US = 60_000L // 60ms window for long files
    }

    /**
     * Extracts normalized peak byte values (0..255) for [bucketCount] time slices.
     * Returns null if decoding fails, duration is unknown, or format is unsupported.
     */
    suspend fun extract(
        context: Context,
        trackUri: Uri,
        durationUs: Long,
        bucketCount: Int = WaveformMath.DEFAULT_BUCKET_COUNT
    ): ByteArray? = withContext(dispatcher) {
        if (durationUs <= 0 || bucketCount <= 0) return@withContext null

        val extractor = MediaExtractor()
        var codec: MediaCodec? = null

        try {
            extractor.setDataSource(context, trackUri, null)

            // Select first audio track
            var audioTrackIndex = -1
            var audioFormat: MediaFormat? = null
            var mime: String? = null

            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val trackMime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (trackMime.startsWith("audio/")) {
                    audioTrackIndex = i
                    audioFormat = format
                    mime = trackMime
                    break
                }
            }

            if (audioTrackIndex == -1 || audioFormat == null || mime == null) {
                return@withContext null
            }

            extractor.selectTrack(audioTrackIndex)
            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(audioFormat, null, null, 0)
            codec.start()

            val rawPeaks = FloatArray(bucketCount) { 0f }
            val isLongAudio = durationUs > LONG_AUDIO_THRESHOLD_US
            val bucketDurationUs = durationUs / bucketCount

            val bufferInfo = MediaCodec.BufferInfo()
            var isInputEos = false
            var isOutputEos = false

            var currentTargetBucket = 0
            var nextWindowTargetUs = 0L

            if (isLongAudio) {
                extractor.seekTo(0L, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
            }

            while (!isOutputEos && coroutineContext.isActive) {
                // 1. Feed input buffer to decoder
                if (!isInputEos) {
                    val inputBufferIndex = codec.dequeueInputBuffer(TIMEOUT_US)
                    if (inputBufferIndex >= 0) {
                        val inputBuffer = codec.getInputBuffer(inputBufferIndex)
                        if (inputBuffer != null) {
                            val sampleSize = extractor.readSampleData(inputBuffer, 0)
                            if (sampleSize < 0) {
                                codec.queueInputBuffer(inputBufferIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                isInputEos = true
                            } else {
                                val sampleTime = extractor.sampleTime
                                codec.queueInputBuffer(inputBufferIndex, 0, sampleSize, sampleTime, 0)
                                extractor.advance()

                                // For long audio, skip ahead to next bucket window
                                if (isLongAudio && sampleTime > nextWindowTargetUs + WINDOW_DECODE_US) {
                                    currentTargetBucket++
                                    if (currentTargetBucket >= bucketCount) {
                                        isInputEos = true
                                    } else {
                                        nextWindowTargetUs = currentTargetBucket * bucketDurationUs
                                        extractor.seekTo(nextWindowTargetUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Read PCM output from decoder
                val outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                if (outputBufferIndex >= 0) {
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        isOutputEos = true
                    }

                    val outputBuffer = codec.getOutputBuffer(outputBufferIndex)
                    if (outputBuffer != null && bufferInfo.size > 0) {
                        val outputFormat = codec.outputFormat
                        val channelCount = outputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT, 2).coerceAtLeast(1)

                        outputBuffer.position(bufferInfo.offset)
                        outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                        outputBuffer.order(ByteOrder.LITTLE_ENDIAN)

                        val shortBuffer = outputBuffer.asShortBuffer()
                        val sampleCount = shortBuffer.remaining()
                        val frameCount = sampleCount / channelCount

                        val sampleTimeUs = bufferInfo.presentationTimeUs
                        val bucketIndex = ((sampleTimeUs * bucketCount) / durationUs).toInt().coerceIn(0, bucketCount - 1)

                        var localMax = 0
                        for (f in 0 until frameCount) {
                            var channelSum = 0
                            for (c in 0 until channelCount) {
                                channelSum += shortBuffer.get()
                            }
                            val monoVal = abs(channelSum / channelCount)
                            if (monoVal > localMax) localMax = monoVal
                        }

                        if (localMax > rawPeaks[bucketIndex]) {
                            rawPeaks[bucketIndex] = localMax.toFloat()
                        }
                    }
                    codec.releaseOutputBuffer(outputBufferIndex, false)
                }
            }

            return@withContext WaveformMath.normalizePeaks(rawPeaks)
        } catch (_: Exception) {
            return@withContext null
        } finally {
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            runCatching { extractor.release() }
        }
    }
}

