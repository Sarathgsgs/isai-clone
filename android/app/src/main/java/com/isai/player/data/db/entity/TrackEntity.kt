package com.isai.player.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tracks",
    indices = [
        Index(value = ["uri"], unique = true),
        Index(value = ["title_sort"]),
        Index(value = ["artist"]),
        Index(value = ["album"]),
        Index(value = ["folder_path"]),
        Index(value = ["is_missing"]),
        Index(value = ["date_added"]),
        Index(value = ["relative_path_norm"])
    ]
)
data class TrackEntity(
    @PrimaryKey
    val id: Long, // 64-bit FNV-1a hash of uri

    val uri: String,
    val title: String,

    @ColumnInfo(name = "title_sort")
    val titleSort: String,

    val artist: String,

    @ColumnInfo(name = "album_artist")
    val albumArtist: String,

    val album: String,
    val year: Int,

    @ColumnInfo(name = "track_no")
    val trackNo: Int,

    @ColumnInfo(name = "disc_no")
    val discNo: Int,

    @ColumnInfo(name = "duration_ms")
    val durationMs: Long,

    @ColumnInfo(name = "mime_type")
    val mimeType: String,

    @ColumnInfo(name = "size_bytes")
    val sizeBytes: Long,

    @ColumnInfo(name = "last_modified")
    val lastModified: Long,

    @ColumnInfo(name = "art_path")
    val artPath: String?,

    @ColumnInfo(name = "folder_path")
    val folderPath: String,

    @ColumnInfo(name = "date_added")
    val dateAdded: Long,

    @ColumnInfo(name = "is_missing", defaultValue = "0")
    val isMissing: Boolean = false,

    @ColumnInfo(name = "relative_path")
    val relativePath: String? = null,

    @ColumnInfo(name = "relative_path_norm")
    val relativePathNorm: String? = null,

    @ColumnInfo(name = "lyrics_uri")
    val lyricsUri: String? = null,

    @ColumnInfo(name = "has_lyrics", defaultValue = "0")
    val hasLyrics: Boolean = false
)

