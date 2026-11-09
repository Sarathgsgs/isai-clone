package com.isai.player.data.model

import androidx.compose.runtime.Immutable

@Immutable
data class Playlist(
    val id: Long,
    val name: String,
    val trackCount: Int,
    val createdAt: Long,
    val updatedAt: Long
)

