package com.isai.player.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "lyrics",
    foreignKeys = [
        ForeignKey(
            entity = TrackEntity::class,
            parentColumns = ["id"],
            childColumns = ["track_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class LyricsEntity(
    @PrimaryKey
    @ColumnInfo(name = "track_id")
    val trackId: Long,

    val source: String, // IMPORTED, SIDECAR, EMBEDDED

    val format: String, // SYNCED, PLAIN

    val content: String,

    @ColumnInfo(name = "offset_ms", defaultValue = "0")
    val offsetMs: Int = 0,

    @ColumnInfo(name = "last_modified")
    val lastModified: Long = System.currentTimeMillis()
)

