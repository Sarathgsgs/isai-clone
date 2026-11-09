package com.isai.player.data.model

import androidx.compose.runtime.Immutable

@Immutable
data class HistoryEntry(
    val id: Long,
    val track: Track,
    val playedAt: Long
)

@Immutable
data class QueueSnapshot(
    val tracks: List<Track>,
    val currentIndex: Int = 0
)

