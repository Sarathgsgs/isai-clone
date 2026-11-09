package com.isai.player.data.db.projection

import androidx.room.ColumnInfo
import androidx.room.Embedded
import com.isai.player.data.db.entity.PlaylistEntity

data class PlaylistWithCount(
    @Embedded
    val playlist: PlaylistEntity,

    @ColumnInfo(name = "track_count")
    val trackCount: Int
)

