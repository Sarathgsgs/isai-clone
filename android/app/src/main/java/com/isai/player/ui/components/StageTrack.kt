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

fun com.isai.player.data.model.Track.toStageTrack(): StageTrack = StageTrack(
    id = id.toString(),
    title = title,
    artist = artist,
    album = album,
    year = year,
    art = artPath
)

