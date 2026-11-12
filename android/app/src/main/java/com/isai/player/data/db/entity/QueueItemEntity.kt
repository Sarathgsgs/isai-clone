package com.isai.player.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "queue_items",
    indices = [
        Index(value = ["track_id"])
    ]
)
data class QueueItemEntity(
    @PrimaryKey
    val position: Int,

    @ColumnInfo(name = "track_id")
    val trackId: Long
)

