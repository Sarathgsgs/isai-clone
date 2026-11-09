package com.isai.player.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "waveforms",
    foreignKeys = [
        ForeignKey(
            entity = TrackEntity::class,
            parentColumns = ["id"],
            childColumns = ["track_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class WaveformEntity(
    @PrimaryKey
    @ColumnInfo(name = "track_id")
    val trackId: Long,

    @ColumnInfo(name = "bucket_count")
    val bucketCount: Int,

    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val peaks: ByteArray,

    val version: Int,

    @ColumnInfo(name = "created_at")
    val createdAt: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WaveformEntity
        if (trackId != other.trackId) return false
        if (bucketCount != other.bucketCount) return false
        if (!peaks.contentEquals(other.peaks)) return false
        if (version != other.version) return false
        if (createdAt != other.createdAt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = trackId.hashCode()
        result = 31 * result + bucketCount
        result = 31 * result + peaks.contentHashCode()
        result = 31 * result + version
        result = 31 * result + createdAt.hashCode()
        return result
    }
}

