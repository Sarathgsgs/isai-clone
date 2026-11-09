package com.isai.player.data.model

import androidx.compose.runtime.Immutable

@Immutable
data class Album(
    val title: String,
    val artist: String,
    val trackCount: Int,
    val year: Int,
    val artPath: String?
) {
    val displayTitle: String
        get() = title.ifBlank { "Unknown Album" }

    val displayArtist: String
        get() = artist.ifBlank { "Unknown Artist" }
}

