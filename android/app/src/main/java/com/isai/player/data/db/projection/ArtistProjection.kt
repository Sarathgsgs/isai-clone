package com.isai.player.data.db.projection

import androidx.room.ColumnInfo

data class ArtistProjection(
    val artist: String,

    @ColumnInfo(name = "track_count")
    val trackCount: Int,

    @ColumnInfo(name = "album_count")
    val albumCount: Int,

    @ColumnInfo(name = "sample_art_path")
    val sampleArtPath: String?
)

