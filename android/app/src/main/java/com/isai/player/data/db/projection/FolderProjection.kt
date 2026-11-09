package com.isai.player.data.db.projection

import androidx.room.ColumnInfo

data class FolderProjection(
    @ColumnInfo(name = "folder_path")
    val folderPath: String,

    @ColumnInfo(name = "track_count")
    val trackCount: Int
)

