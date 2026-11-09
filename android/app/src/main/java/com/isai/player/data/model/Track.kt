package com.isai.player.data.model

import androidx.compose.runtime.Immutable

@Immutable
data class Track(
    val id: Long,
    val uri: String,
    val title: String,
    val titleSort: String,
    val artist: String,
    val albumArtist: String,
    val album: String,
    val year: Int,
    val trackNo: Int,
    val discNo: Int,
    val durationMs: Long,
    val mimeType: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val artPath: String?,
    val folderPath: String,
    val dateAdded: Long,
    val isMissing: Boolean = false,
    val relativePath: String? = null,
    val lyricsUri: String? = null,
    val hasLyrics: Boolean = false
) {
    val displayArtist: String
        get() = artist.ifBlank { "Unknown Artist" }

    val displayAlbum: String
        get() = album.ifBlank { "Unknown Album" }

    val displayTitle: String
        get() = title.ifBlank { "Unknown Title" }
}

