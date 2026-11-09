package com.isai.player.data.lyrics

import java.util.regex.Pattern

/**
 * High-performance, fault-tolerant parser for standard LRC, enhanced LRC, and plain-text lyrics.
 *
 * Supports:
 * - Standard [mm:ss.xx] and [mm:ss.xxx] timestamps
 * - Multi-timestamp lines (e.g. [00:12.00][00:34.00]Same lyrics)
 * - Metadata header tags [ti:], [ar:], [al:], [offset:±ms]
 * - Word-level timestamps <mm:ss.xx>
 * - UTF-8 BOM (\uFEFF) stripping and encoding resilience
 * - O(log N) binary search lookup for active line calculation
 */
object LrcParser {

    private val TIME_TAG_REGEX = Pattern.compile("\\[(\\d{1,2}):(\\d{2})(?:[.:](\\d{2,3}))?\\]")
    private val HEADER_TAG_REGEX = Pattern.compile("\\[([a-zA-Z]+):([^\\]]*)\\]")
    private val WORD_TIME_REGEX = Pattern.compile("<(\\d{1,2}):(\\d{2})(?:[.:](\\d{2,3}))?>([^<]+)")

    /**
     * Parses raw LRC or plain text into a structured [ParsedLyrics] object.
     * Never throws exceptions on malformed lines; skips errors gracefully.
     */
    fun parse(rawText: String): ParsedLyrics {
        if (rawText.isBlank()) {
            return ParsedLyrics(format = LyricsFormat.PLAIN, lines = emptyList())
        }

        // Strip UTF-8 Byte Order Mark (BOM) if present
        val cleanText = rawText.removePrefix("\uFEFF").replace("\r\n", "\n").replace('\r', '\n')
        val rawLines = cleanText.lines()

        val parsedLines = mutableListOf<LyricsLine>()
        var offsetMs = 0
        var title: String? = null
        var artist: String? = null
        var album: String? = null
        var hasAnyTimestamp = false

        for (line in rawLines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue

            // 1. Check for metadata header tags [ti:], [ar:], [offset:]
            val headerMatcher = HEADER_TAG_REGEX.matcher(trimmed)
            if (headerMatcher.matches()) {
                val key = headerMatcher.group(1)?.lowercase() ?: ""
                val value = headerMatcher.group(2)?.trim() ?: ""
                when (key) {
                    "ti" -> title = value
                    "ar" -> artist = value
                    "al" -> album = value
                    "offset" -> offsetMs = value.toIntOrNull() ?: 0
                }
                continue
            }

            // 2. Check for timestamp tags (including multiple timestamps on one line)
            val timeMatcher = TIME_TAG_REGEX.matcher(trimmed)
            val timestamps = mutableListOf<Long>()
            var lastMatchEnd = 0

            while (timeMatcher.find()) {
                val minStr = timeMatcher.group(1) ?: "0"
                val secStr = timeMatcher.group(2) ?: "0"
                val fracStr = timeMatcher.group(3) ?: "0"

                val minutes = minStr.toLongOrNull() ?: 0L
                val seconds = secStr.toLongOrNull() ?: 0L
                val fractionMs = when (fracStr.length) {
                    2 -> (fracStr.toLongOrNull() ?: 0L) * 10L // Centiseconds -> ms
                    3 -> fracStr.toLongOrNull() ?: 0L        // Milliseconds
                    else -> 0L
                }

                val totalMs = minutes * 60_000L + seconds * 1_000L + fractionMs
                timestamps.add(totalMs)
                lastMatchEnd = timeMatcher.end()
            }

            if (timestamps.isNotEmpty()) {
                hasAnyTimestamp = true
                val lyricText = trimmed.substring(lastMatchEnd).trim()

                // Check for word-level timestamps within lyricText
                val words = parseWordTimestamps(lyricText)
                val cleanLyricText = if (words.isNotEmpty()) {
                    words.joinToString(" ") { it.word }
                } else {
                    lyricText
                }

                for (ts in timestamps) {
                    parsedLines.add(LyricsLine(timestampMs = ts, text = cleanLyricText, words = words.ifEmpty { null }))
                }
            } else {
                // Line without timestamps (plain text fallback)
                parsedLines.add(LyricsLine(timestampMs = -1L, text = trimmed))
            }
        }

        return if (hasAnyTimestamp) {
            // Synced lyrics: filter out plain lines, sort by timestamp
            val synced = parsedLines.filter { it.timestampMs >= 0L }.sortedBy { it.timestampMs }
            ParsedLyrics(
                format = LyricsFormat.SYNCED,
                lines = synced,
                offsetMs = offsetMs,
                title = title,
                artist = artist,
                album = album
            )
        } else {
            // Plain text lyrics
            ParsedLyrics(
                format = LyricsFormat.PLAIN,
                lines = parsedLines,
                offsetMs = offsetMs,
                title = title,
                artist = artist,
                album = album
            )
        }
    }

    /**
     * Extracts word-level timestamps <mm:ss.xx>word from enhanced LRC text.
     */
    private fun parseWordTimestamps(text: String): List<WordTimestamp> {
        val matcher = WORD_TIME_REGEX.matcher(text)
        val words = mutableListOf<WordTimestamp>()

        var previousStartMs = -1L
        var previousWord: String? = null

        while (matcher.find()) {
            val minStr = matcher.group(1) ?: "0"
            val secStr = matcher.group(2) ?: "0"
            val fracStr = matcher.group(3) ?: "0"
            val word = matcher.group(4)?.trim() ?: ""

            val minutes = minStr.toLongOrNull() ?: 0L
            val seconds = secStr.toLongOrNull() ?: 0L
            val fractionMs = if (fracStr.length == 2) (fracStr.toLongOrNull() ?: 0L) * 10L else fracStr.toLongOrNull() ?: 0L
            val currentMs = minutes * 60_000L + seconds * 1_000L + fractionMs

            if (previousStartMs >= 0L && previousWord != null) {
                words.add(WordTimestamp(startMs = previousStartMs, endMs = currentMs, word = previousWord))
            }
            previousStartMs = currentMs
            previousWord = word
        }

        if (previousStartMs >= 0L && previousWord != null) {
            words.add(WordTimestamp(startMs = previousStartMs, endMs = previousStartMs + 1000L, word = previousWord))
        }

        return words
    }

    /**
     * Finds the index of the active lyric line at [timeMs] using O(log N) binary search.
     * Returns -1 if [timeMs] is before the first line.
     */
    fun lineIndexAt(lines: List<LyricsLine>, timeMs: Long): Int {
        if (lines.isEmpty()) return -1
        if (timeMs < lines[0].timestampMs) return -1

        var low = 0
        var high = lines.size - 1
        var bestIndex = -1

        while (low <= high) {
            val mid = (low + high) ushr 1
            val midTime = lines[mid].timestampMs

            if (midTime <= timeMs) {
                bestIndex = mid
                low = mid + 1 // Search later to find the closest line <= timeMs
            } else {
                high = mid - 1
            }
        }
        return bestIndex
    }
}

