package com.isai.player.data.m3u

import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

data class M3uEntry(
    val rawPath: String,
    val normalizedPath: String,
    val durationSeconds: Int? = null,
    val title: String? = null,
    val artist: String? = null
)

data class M3uPlaylist(
    val name: String? = null,
    val entries: List<M3uEntry> = emptyList()
)

object M3uParser {

    private const val MAX_ENTRIES = 50_000
    private const val MAX_BYTES = 5 * 1024 * 1024 // 5 MB

    fun parse(content: String): M3uPlaylist {
        return parse(content.toByteArray(StandardCharsets.UTF_8))
    }

    fun parse(bytes: ByteArray): M3uPlaylist {
        val safeBytes = if (bytes.size > MAX_BYTES) bytes.copyOf(MAX_BYTES) else bytes

        // Check and strip UTF-8 BOM (0xEF, 0xBB, 0xBF)
        val (data, charset) = detectCharsetAndStripBom(safeBytes)
        return parseStream(ByteArrayInputStream(data), charset)
    }

    fun parse(inputStream: InputStream): M3uPlaylist {
        return parseStream(inputStream, StandardCharsets.UTF_8)
    }

    private fun parseStream(inputStream: InputStream, charset: Charset): M3uPlaylist {
        val reader = BufferedReader(InputStreamReader(inputStream, charset))
        var playlistName: String? = null
        val entries = mutableListOf<M3uEntry>()

        var pendingDuration: Int? = null
        var pendingTitle: String? = null
        var pendingArtist: String? = null

        var line: String? = reader.readLine()
        while (line != null && entries.size < MAX_ENTRIES) {
            val trimmed = line.trim()

            if (trimmed.isEmpty()) {
                line = reader.readLine()
                continue
            }

            if (trimmed.startsWith("#")) {
                when {
                    trimmed.startsWith("#PLAYLIST:", ignoreCase = true) -> {
                        playlistName = trimmed.substring(10).trim()
                    }
                    trimmed.startsWith("#EXTINF:", ignoreCase = true) -> {
                        val parsedInf = parseExtInf(trimmed.substring(8).trim())
                        pendingDuration = parsedInf.durationSeconds
                        pendingTitle = parsedInf.title
                        pendingArtist = parsedInf.artist
                    }
                }
            } else {
                // Audio file path line
                val normalized = PathNormalizer.normalize(trimmed)
                val entry = M3uEntry(
                    rawPath = trimmed,
                    normalizedPath = normalized,
                    durationSeconds = pendingDuration,
                    title = pendingTitle,
                    artist = pendingArtist
                )
                entries.add(entry)

                // Reset pending metadata
                pendingDuration = null
                pendingTitle = null
                pendingArtist = null
            }

            line = reader.readLine()
        }

        return M3uPlaylist(name = playlistName, entries = entries)
    }

    private data class ExtInfData(
        val durationSeconds: Int?,
        val artist: String?,
        val title: String?
    )

    private fun parseExtInf(content: String): ExtInfData {
        // Format: [duration],[artist - title] or [duration],[title]
        val commaIndex = content.indexOf(',')
        if (commaIndex == -1) {
            val duration = content.toIntOrNull()?.takeIf { it >= 0 }
            return ExtInfData(durationSeconds = duration, artist = null, title = null)
        }

        val durationPart = content.substring(0, commaIndex).trim()
        val duration = durationPart.substringBefore(' ').toIntOrNull()?.takeIf { it >= 0 }

        val infoPart = content.substring(commaIndex + 1).trim()
        return if (infoPart.contains(" - ")) {
            val artist = infoPart.substringBefore(" - ").trim().takeIf { it.isNotBlank() }
            val title = infoPart.substringAfter(" - ").trim().takeIf { it.isNotBlank() }
            ExtInfData(durationSeconds = duration, artist = artist, title = title)
        } else {
            ExtInfData(durationSeconds = duration, artist = null, title = infoPart.takeIf { it.isNotBlank() })
        }
    }

    private fun detectCharsetAndStripBom(bytes: ByteArray): Pair<ByteArray, Charset> {
        if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) {
            return Pair(bytes.copyOfRange(3, bytes.size), StandardCharsets.UTF_8)
        }
        return Pair(bytes, StandardCharsets.UTF_8)
    }
}

