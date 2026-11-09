package com.isai.player.data.model

import androidx.compose.runtime.Immutable

@Immutable
data class Artist(
    val name: String,
    val trackCount: Int,
    val albumCount: Int,
    val sampleArtPath: String?
) {
    val displayName: String
        get() = name.ifBlank { "Unknown Artist" }
}

