package com.isai.player.data.m3u

import java.net.URLDecoder
import java.util.Locale

object PathNormalizer {

    /**
     * Cleans and canonicalizes an audio file path from an M3U entry or local filesystem:
     * - Strips file:// schemes
     * - URL-decodes percent encodings (%20 -> space, etc.)
     * - Converts Windows backslashes (\) to forward slashes (/)
     * - Strips Windows drive prefixes (e.g. "C:\", "D:/")
     * - Strips leading relative tokens ("./") and redundant slashes
     */
    fun normalize(rawPath: String): String {
        var path = rawPath.trim()

        if (path.startsWith("file://", ignoreCase = true)) {
            path = path.substring(7)
        }

        // URL decoding
        path = runCatching { URLDecoder.decode(path, "UTF-8") }.getOrDefault(path)

        // Convert Windows backslashes
        path = path.replace('\\', '/')

        // Strip Windows drive letters (e.g. "C:/", "D:")
        path = path.replace(Regex("""^[a-zA-Z]:/*"""), "")

        // Collapse redundant slashes
        path = path.replace(Regex("""/{2,}"""), "/")

        // Strip leading ./ and /
        while (path.startsWith("./") || path.startsWith("/")) {
            path = if (path.startsWith("./")) path.substring(2) else path.substring(1)
        }

        return path.trim()
    }

    /**
     * Canonical lowercased path used for database indexing and case-insensitive matching.
     */
    fun normalizeForSearch(rawPath: String): String {
        return normalize(rawPath).lowercase(Locale.ROOT)
    }

    /**
     * Extracts just the file name (e.g. "01 Song.flac") from a path.
     */
    fun getFileName(rawPath: String): String {
        val normalized = normalize(rawPath)
        return normalized.substringAfterLast('/')
    }

    /**
     * Extracts trailing path segments (e.g. last 2 segments: "Album/Song.mp3") for suffix matching.
     */
    fun getTrailingSegments(rawPath: String, segmentCount: Int = 2): String {
        val normalized = normalize(rawPath)
        val segments = normalized.split('/').filter { it.isNotBlank() }
        if (segments.size <= segmentCount) return normalized
        return segments.takeLast(segmentCount).joinToString("/")
    }
}

