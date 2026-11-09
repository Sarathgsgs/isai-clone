package com.isai.player.playback

import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.isai.player.data.model.Track
import java.io.File

object MediaItemMapper {
    private const val EXTRA_TRACK_ID = "extra_track_id"
    private const val EXTRA_TRACK_NO = "extra_track_no"
    private const val EXTRA_DISC_NO = "extra_disc_no"
    private const val EXTRA_YEAR = "extra_year"
    private const val EXTRA_DURATION = "extra_duration"
    private const val EXTRA_ART_PATH = "extra_art_path"
    private const val EXTRA_FOLDER_PATH = "extra_folder_path"

    fun toMediaItem(track: Track): MediaItem {
        val extras = Bundle().apply {
            putLong(EXTRA_TRACK_ID, track.id)
            putInt(EXTRA_TRACK_NO, track.trackNo)
            putInt(EXTRA_DISC_NO, track.discNo)
            putInt(EXTRA_YEAR, track.year)
            putLong(EXTRA_DURATION, track.durationMs)
            putString(EXTRA_ART_PATH, track.artPath)
            putString(EXTRA_FOLDER_PATH, track.folderPath)
        }

        val artworkUri = track.artPath?.let { path ->
            val file = File(path)
            if (file.exists()) Uri.fromFile(file) else null
        }

        val metadata = MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.artist)
            .setAlbumTitle(track.album)
            .setAlbumArtist(track.albumArtist)
            .setArtworkUri(artworkUri)
            .setTrackNumber(track.trackNo)
            .setDiscNumber(track.discNo)
            .setRecordingYear(track.year)
            .setExtras(extras)
            .setIsPlayable(true)
            .build()

        return MediaItem.Builder()
            .setMediaId(track.id.toString())
            .setUri(Uri.parse(track.uri))
            .setMediaMetadata(metadata)
            .build()
    }

    fun toTrack(mediaItem: MediaItem): Track? {
        val metadata = mediaItem.mediaMetadata
        val extras = metadata.extras
        val id = extras?.getLong(EXTRA_TRACK_ID)
            ?: mediaItem.mediaId.toLongOrNull()
            ?: return null

        val uri = mediaItem.requestMetadata.mediaUri?.toString()
            ?: mediaItem.localConfiguration?.uri?.toString()
            ?: ""

        return Track(
            id = id,
            uri = uri,
            title = metadata.title?.toString() ?: "Unknown Title",
            titleSort = metadata.title?.toString()?.lowercase() ?: "",
            artist = metadata.artist?.toString() ?: "Unknown Artist",
            albumArtist = metadata.albumArtist?.toString() ?: metadata.artist?.toString() ?: "",
            album = metadata.albumTitle?.toString() ?: "Unknown Album",
            year = metadata.recordingYear ?: extras?.getInt(EXTRA_YEAR) ?: 0,
            trackNo = metadata.trackNumber ?: extras?.getInt(EXTRA_TRACK_NO) ?: 1,
            discNo = metadata.discNumber ?: extras?.getInt(EXTRA_DISC_NO) ?: 1,
            durationMs = extras?.getLong(EXTRA_DURATION) ?: 0L,
            mimeType = "audio/*",
            sizeBytes = 0L,
            lastModified = 0L,
            artPath = extras?.getString(EXTRA_ART_PATH),
            folderPath = extras?.getString(EXTRA_FOLDER_PATH) ?: "Music",
            dateAdded = 0L
        )
    }
}

