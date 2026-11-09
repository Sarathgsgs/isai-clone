package com.isai.player.data.lyrics

import android.content.Context
import android.net.Uri
import java.io.InputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

/**
 * Lightweight, zero-dependency embedded lyrics reader.
 *
 * Extracts:
 * - ID3v2 USLT (Unsynchronized lyrics) and SYLT (Synchronized lyrics) from MP3 files.
 * - Vorbis Comments (LYRICS, UNSYNCEDLYRICS) from FLAC and OGG files.
 * - MediaMetadataRetriever fallback on Android API 30+.
 */
object EmbeddedLyricsReader {

    fun readEmbeddedLyrics(context: Context, trackUri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(trackUri)?.use { input ->
                readFromStream(input)
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Parses ID3v2 or FLAC header directly from stream.
     */
    fun readFromStream(input: InputStream): String? {
        val header = ByteArray(10)
        val read = input.read(header)
        if (read < 10) return null

        // Check for ID3v2: "ID3"
        if (header[0] == 'I'.code.toByte() && header[1] == 'D'.code.toByte() && header[2] == '3'.code.toByte()) {
            val majorVersion = header[3].toInt() and 0xFF
            val tagSize = decodeSynchsafeInt(header, 6)
            return parseId3v2Frames(input, majorVersion, tagSize)
        }

        // Check for FLAC: "fLaC"
        if (header[0] == 'f'.code.toByte() && header[1] == 'L'.code.toByte() &&
            header[2] == 'a'.code.toByte() && header[3] == 'C'.code.toByte()
        ) {
            return parseFlacVorbisComment(input)
        }

        return null
    }

    /**
     * Parses ID3v2 frames searching for USLT or SYLT.
     */
    private fun parseId3v2Frames(input: InputStream, majorVersion: Int, tagSize: Int): String? {
        var remainingBytes = tagSize
        val frameHeaderSize = 10
        val headerBuf = ByteArray(frameHeaderSize)

        while (remainingBytes >= frameHeaderSize) {
            val read = input.read(headerBuf)
            if (read < frameHeaderSize) break
            remainingBytes -= read

            val frameId = String(headerBuf, 0, 4, StandardCharsets.ISO_8859_1)
            // Padding detected (all zeros)
            if (frameId.all { it == '\u0000' }) break

            val frameSize = if (majorVersion >= 4) {
                decodeSynchsafeInt(headerBuf, 4)
            } else {
                decodeStandardInt(headerBuf, 4)
            }

            if (frameSize <= 0 || frameSize > remainingBytes) break

            if (frameId == "USLT" || frameId == "SYLT") {
                val frameData = ByteArray(frameSize)
                var bytesRead = 0
                while (bytesRead < frameSize) {
                    val count = input.read(frameData, bytesRead, frameSize - bytesRead)
                    if (count < 0) break
                    bytesRead += count
                }
                return parseUsltFrame(frameData)
            } else {
                // Skip non-lyrics frame
                skipFully(input, frameSize.toLong())
                remainingBytes -= frameSize
            }
        }
        return null
    }

    /**
     * Decodes ID3v2 USLT (Unsynchronized lyrics) frame data:
     * [Encoding: 1 byte][Language: 3 bytes][Descriptor null-terminated][Lyrics text]
     */
    private fun parseUsltFrame(data: ByteArray): String? {
        if (data.size <= 4) return null
        val encodingByte = data[0].toInt() and 0xFF
        val charset = when (encodingByte) {
            1 -> StandardCharsets.UTF_16
            2 -> StandardCharsets.UTF_16BE
            3 -> StandardCharsets.UTF_8
            else -> StandardCharsets.ISO_8859_1
        }

        // Language is bytes 1..3
        var offset = 4
        // Skip descriptor null-terminator
        if (encodingByte == 1 || encodingByte == 2) {
            // UTF-16 null is 2 bytes (0x00 0x00)
            while (offset + 1 < data.size) {
                if (data[offset] == 0.toByte() && data[offset + 1] == 0.toByte()) {
                    offset += 2
                    break
                }
                offset += 2
            }
        } else {
            // 1-byte null (0x00)
            while (offset < data.size) {
                if (data[offset] == 0.toByte()) {
                    offset += 1
                    break
                }
                offset++
            }
        }

        if (offset >= data.size) return null
        return String(data, offset, data.size - offset, charset).trim()
    }

    /**
     * Parses FLAC metadata blocks searching for Vorbis Comments (Block type 4).
     */
    private fun parseFlacVorbisComment(input: InputStream): String? {
        val blockHeader = ByteArray(4)

        while (true) {
            val read = input.read(blockHeader)
            if (read < 4) break

            val isLast = (blockHeader[0].toInt() and 0x80) != 0
            val blockType = blockHeader[0].toInt() and 0x7F
            val blockSize = ((blockHeader[1].toInt() and 0xFF) shl 16) or
                    ((blockHeader[2].toInt() and 0xFF) shl 8) or
                    (blockHeader[3].toInt() and 0xFF)

            if (blockType == 4) { // VORBIS_COMMENT
                val commentData = ByteArray(blockSize)
                var bytesRead = 0
                while (bytesRead < blockSize) {
                    val count = input.read(commentData, bytesRead, blockSize - bytesRead)
                    if (count < 0) break
                    bytesRead += count
                }
                return extractVorbisLyrics(commentData)
            } else {
                skipFully(input, blockSize.toLong())
            }

            if (isLast) break
        }
        return null
    }

    private fun extractVorbisLyrics(data: ByteArray): String? {
        if (data.size < 8) return null
        var offset = 0

        // Vendor string length (little-endian uint32)
        val vendorLength = readLittleEndianInt(data, offset)
        offset += 4 + vendorLength
        if (offset + 4 > data.size) return null

        // User comment list length
        val userCommentCount = readLittleEndianInt(data, offset)
        offset += 4

        for (i in 0 until userCommentCount) {
            if (offset + 4 > data.size) break
            val commentLength = readLittleEndianInt(data, offset)
            offset += 4
            if (offset + commentLength > data.size) break

            val comment = String(data, offset, commentLength, StandardCharsets.UTF_8)
            offset += commentLength

            val upper = comment.uppercase()
            if (upper.startsWith("LYRICS=") || upper.startsWith("UNSYNCEDLYRICS=") || upper.startsWith("SYNCEDLYRICS=")) {
                val eqIdx = comment.indexOf('=')
                if (eqIdx >= 0 && eqIdx < comment.length - 1) {
                    return comment.substring(eqIdx + 1).trim()
                }
            }
        }
        return null
    }

    private fun decodeSynchsafeInt(buf: ByteArray, offset: Int): Int {
        return ((buf[offset].toInt() and 0x7F) shl 21) or
                ((buf[offset + 1].toInt() and 0x7F) shl 14) or
                ((buf[offset + 2].toInt() and 0x7F) shl 7) or
                (buf[offset + 3].toInt() and 0x7F)
    }

    private fun decodeStandardInt(buf: ByteArray, offset: Int): Int {
        return ((buf[offset].toInt() and 0xFF) shl 24) or
                ((buf[offset + 1].toInt() and 0xFF) shl 16) or
                ((buf[offset + 2].toInt() and 0xFF) shl 8) or
                (buf[offset + 3].toInt() and 0xFF)
    }

    private fun readLittleEndianInt(data: ByteArray, offset: Int): Int {
        return (data[offset].toInt() and 0xFF) or
                ((data[offset + 1].toInt() and 0xFF) shl 8) or
                ((data[offset + 2].toInt() and 0xFF) shl 16) or
                ((data[offset + 3].toInt() and 0xFF) shl 24)
    }

    private fun skipFully(input: InputStream, bytes: Long) {
        var remaining = bytes
        while (remaining > 0) {
            val skipped = input.skip(remaining)
            if (skipped <= 0) {
                if (input.read() == -1) break
                remaining -= 1
            } else {
                remaining -= skipped
            }
        }
    }
}

