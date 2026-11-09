package com.isai.player.data.lyrics

/**
 * Origin of track lyrics.
 * Prioritized: IMPORTED > SIDECAR > EMBEDDED
 */
enum class LyricsSource {
    IMPORTED,
    SIDECAR,
    EMBEDDED
}

/**
 * Format of lyrics content.
 */
enum class LyricsFormat {
    SYNCED,
    PLAIN
}

/**
 * Represents a single line of lyrics with an optional millisecond timestamp.
 */
data class LyricsLine(
    val timestampMs: Long,
    val text: String,
    val words: List<WordTimestamp>? = null
)

/**
 * Optional word-level timestamp for enhanced LRC files.
 */
data class WordTimestamp(
    val startMs: Long,
    val endMs: Long,
    val word: String
)

/**
 * Parsed lyrics containing sorted lines and extracted metadata tags.
 */
data class ParsedLyrics(
    val format: LyricsFormat,
    val lines: List<LyricsLine>,
    val offsetMs: Int = 0,
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null
)

