package com.isai.player.ui.components

import androidx.compose.runtime.Immutable

enum class SwipeDirection {
    NEXT,
    PREVIOUS,
    NONE
}

@Immutable
data class StageTrack(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val year: Int,
    val art: Any?
)
