package com.isai.player.data.m3u

import com.isai.player.data.model.Track
import java.io.BufferedWriter
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

object M3uWriter {

    /**
     * Serializes a playlist of tracks into standard UTF-8 M3U8 format.
     */
    fun write(playlistName: String, tracks: List<Track>): String {
        val sb = StringBuilder()
        sb.append("#EXTM3U\n")
        if (playlistName.isNotBlank()) {
            sb.append("#PLAYLIST:").append(playlistName.trim()).append("\n")
        }

        for (track in tracks) {
            val durationSec = (track.durationMs / 1000L).toInt().coerceAtLeast(0)
            val artist = track.displayArtist.replace(',', ' ').trim()
            val title = track.displayTitle.replace(',', ' ').trim()

            sb.append("#EXTINF:").append(durationSec).append(",").append(artist).append(" - ").append(title).append("\n")

            // Prefer relative path if present; fallback to URI
            val path = track.relativePath?.takeIf { it.isNotBlank() } ?: track.uri
            sb.append(path).append("\n")
        }

        return sb.toString()
    }

    /**
     * Writes serialized M3U8 data directly to an OutputStream.
     */
    fun writeToStream(playlistName: String, tracks: List<Track>, outputStream: OutputStream) {
        val writer = BufferedWriter(OutputStreamWriter(outputStream, StandardCharsets.UTF_8))
        writer.write(write(playlistName, tracks))
        writer.flush()
    }
}

