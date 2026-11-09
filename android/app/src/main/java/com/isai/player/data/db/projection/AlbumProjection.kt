package com.isai.player.data.db.projection

import androidx.room.ColumnInfo

data class AlbumProjection(
    val album: String,

    @ColumnInfo(name = "album_artist")
    val albumArtist: String,

    @ColumnInfo(name = "track_count")
    val trackCount: Int,

    @ColumnInfo(name = "min_year")
    val year: Int,

    @ColumnInfo(name = "art_path")
    val artPath: String?
)

