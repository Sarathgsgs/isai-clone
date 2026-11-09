package com.isai.player.data.model

import com.isai.player.data.db.entity.PlaylistEntity
import com.isai.player.data.db.entity.TrackEntity
import com.isai.player.data.db.projection.AlbumProjection
import com.isai.player.data.db.projection.ArtistProjection
import com.isai.player.data.db.projection.FolderProjection
import com.isai.player.data.db.projection.PlaylistWithCount

fun TrackEntity.toDomain(): Track = Track(
    id = id,
    uri = uri,
    title = title,
    titleSort = titleSort,
    artist = artist,
    albumArtist = albumArtist,
    album = album,
    year = year,
    trackNo = trackNo,
    discNo = discNo,
    durationMs = durationMs,
    mimeType = mimeType,
    sizeBytes = sizeBytes,
    lastModified = lastModified,
    artPath = artPath,
    folderPath = folderPath,
    dateAdded = dateAdded,
    isMissing = isMissing,
    relativePath = relativePath,
    lyricsUri = lyricsUri,
    hasLyrics = hasLyrics
)

fun Track.toEntity(): TrackEntity = TrackEntity(
    id = id,
    uri = uri,
    title = title,
    titleSort = titleSort,
    artist = artist,
    albumArtist = albumArtist,
    album = album,
    year = year,
    trackNo = trackNo,
    discNo = discNo,
    durationMs = durationMs,
    mimeType = mimeType,
    sizeBytes = sizeBytes,
    lastModified = lastModified,
    artPath = artPath,
    folderPath = folderPath,
    dateAdded = dateAdded,
    isMissing = isMissing,
    relativePath = relativePath,
    relativePathNorm = relativePath?.lowercase(),
    lyricsUri = lyricsUri,
    hasLyrics = hasLyrics
)

fun AlbumProjection.toDomain(): Album = Album(
    title = album,
    artist = albumArtist,
    trackCount = trackCount,
    year = year,
    artPath = artPath
)

fun ArtistProjection.toDomain(): Artist = Artist(
    name = artist,
    trackCount = trackCount,
    albumCount = albumCount,
    sampleArtPath = sampleArtPath
)

fun FolderProjection.toDomain(): Folder {
    val folderName = folderPath.trimEnd('/').substringAfterLast('/', folderPath)
    return Folder(
        path = folderPath,
        name = if (folderName.isBlank()) "Music" else folderName,
        trackCount = trackCount
    )
}

fun PlaylistWithCount.toDomain(): Playlist = Playlist(
    id = playlist.id,
    name = playlist.name,
    trackCount = trackCount,
    createdAt = playlist.createdAt,
    updatedAt = playlist.updatedAt
)

fun PlaylistEntity.toDomain(trackCount: Int = 0): Playlist = Playlist(
    id = id,
    name = name,
    trackCount = trackCount,
    createdAt = createdAt,
    updatedAt = updatedAt
)

