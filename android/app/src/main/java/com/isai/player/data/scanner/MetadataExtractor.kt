package com.isai.player.data.scanner

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.isai.player.data.db.entity.TrackEntity
import com.isai.player.data.db.util.Fnv1a
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MetadataExtractor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val artCache: ArtCache
) {

    /**
     * Extracts audio metadata and embedded artwork from [file], falling back gracefully to
     * filename sanitization when ID3/Vorbis/MP4 tags are missing or corrupt.
     */
    suspend fun extractTrack(file: DiscoveredFile): TrackEntity = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        var rawTitle: String? = null
        var rawArtist: String? = null
        var rawAlbum: String? = null
        var rawAlbumArtist: String? = null
        var rawYear: String? = null
        var rawTrackNumber: String? = null
        var rawDiscNumber: String? = null
        var rawDuration: String? = null
        var embeddedArtBytes: ByteArray? = null

        try {
            retriever.setDataSource(context, Uri.parse(file.uriString))
            rawTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            rawArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            rawAlbum = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
            rawAlbumArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)
            rawYear = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)
                ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE)
            rawTrackNumber = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)
            rawDiscNumber = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER)
            rawDuration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            embeddedArtBytes = retriever.embeddedPicture
        } catch (e: Exception) {
            // Unreadable tags or unsupported container, proceed with filename fallback
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                // Ignore release exceptions
            }
        }

        val title = if (!rawTitle.isNullOrBlank()) {
            rawTitle.trim()
        } else {
            sanitizeFileNameToTitle(file.displayName)
        }

        val artist = if (!rawArtist.isNullOrBlank()) {
            rawArtist.trim()
        } else if (!rawAlbumArtist.isNullOrBlank()) {
            rawAlbumArtist.trim()
        } else {
            "Unknown Artist"
        }

        val albumArtist = if (!rawAlbumArtist.isNullOrBlank()) {
            rawAlbumArtist.trim()
        } else {
            artist
        }

        val album = if (!rawAlbum.isNullOrBlank()) {
            rawAlbum.trim()
        } else {
            "Unknown Album"
        }

        val year = parseYear(rawYear)
        val trackNo = parseCompoundNumber(rawTrackNumber, default = 1)
        val discNo = parseCompoundNumber(rawDiscNumber, default = 1)
        val durationMs = rawDuration?.toLongOrNull()?.coerceAtLeast(0L) ?: 0L

        val artPath = if (embeddedArtBytes != null) {
            artCache.saveEmbeddedArt(file.uriString, embeddedArtBytes)
        } else {
            null
        }

        val titleSort = sanitizeForSort(title)

        val relPath = if (file.folderPath == "Root" || file.folderPath.isBlank()) {
            file.displayName
        } else {
            "${file.folderPath}/${file.displayName}"
        }

        TrackEntity(
            id = Fnv1a.hash64(file.uriString),
            uri = file.uriString,
            title = title,
            titleSort = titleSort,
            artist = artist,
            albumArtist = albumArtist,
            album = album,
            year = year,
            trackNo = trackNo,
            discNo = discNo,
            durationMs = durationMs,
            mimeType = file.mimeType,
            sizeBytes = file.sizeBytes,
            lastModified = file.lastModified,
            artPath = artPath,
            folderPath = file.folderPath,
            dateAdded = System.currentTimeMillis(),
            isMissing = false,
            relativePath = relPath,
            relativePathNorm = relPath.lowercase(Locale.ROOT),
            lyricsUri = null,
            hasLyrics = false
        )
    }

    fun sanitizeFileNameToTitle(displayName: String): String {
        // Strip extension (e.g. .mp3, .flac)
        var name = displayName.substringBeforeLast('.', displayName)
        // Strip leading numbers: "01. ", "01 - ", "1 ", "01_"
        name = name.replace(Regex("""^\d+[\s._-]+"""), "")
        // Replace underscores with spaces
        name = name.replace('_', ' ').trim()
        return if (name.isBlank()) displayName else name
    }

    fun sanitizeForSort(input: String): String {
        var sort = input.trim().lowercase(Locale.ROOT)
        // Strip leading articles for alphabetical sorting: "the ", "a ", "an "
        if (sort.startsWith("the ")) {
            sort = sort.substring(4)
        } else if (sort.startsWith("a ")) {
            sort = sort.substring(2)
        } else if (sort.startsWith("an ")) {
            sort = sort.substring(3)
        }
        return sort.trim()
    }

    fun parseCompoundNumber(raw: String?, default: Int = 1): Int {
        if (raw.isNullOrBlank()) return default
        val firstPart = raw.split('/', '-', ',').firstOrNull()?.trim() ?: return default
        return firstPart.toIntOrNull()?.coerceAtLeast(0) ?: default
    }

    fun parseYear(raw: String?): Int {
        if (raw.isNullOrBlank()) return 0
        // Extract first 4-digit number (e.g. "1991", "2024-05-12", "1991/10")
        val match = Regex("""\b(19\d{2}|20\d{2})\b""").find(raw)
        return match?.value?.toIntOrNull() ?: 0
    }
}

