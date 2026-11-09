package com.isai.player.playback.car

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.isai.player.data.db.IsaiDatabase
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileNotFoundException

class CarArtworkProvider : ContentProvider() {

    companion object {
        const val AUTHORITY_SUFFIX = ".artwork"
        private const val CODE_TRACK_ART = 1

        /**
         * Validates the URI segment against path traversal attacks.
         * Returns valid trackId or throws SecurityException / IllegalArgumentException.
         */
        fun validateAndResolveTrackId(uri: Uri): Long {
            val segment = uri.lastPathSegment
                ?: throw IllegalArgumentException("Missing track ID in URI: $uri")

            if (segment.contains("..") || segment.contains("/") || segment.contains("\\") || segment.contains("%")) {
                throw SecurityException("Path traversal attempt detected in URI: $uri")
            }

            return segment.toLongOrNull()
                ?: throw IllegalArgumentException("Invalid numeric track ID: $segment")
        }

        fun buildArtworkUri(packageName: String, trackId: Long): Uri {
            return Uri.parse("content://$packageName$AUTHORITY_SUFFIX/$trackId")
        }
    }

    private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH)

    override fun onCreate(): Boolean {
        val authority = context?.packageName?.let { "$it$AUTHORITY_SUFFIX" } ?: return false
        uriMatcher.addURI(authority, "#", CODE_TRACK_ART)
        return true
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        if (mode != "r") {
            throw SecurityException("Only read-only mode 'r' is supported")
        }

        val trackId = validateAndResolveTrackId(uri)
        val ctx = context ?: throw FileNotFoundException("Context unavailable")

        // Retrieve track artwork path from database
        val db = androidx.room.Room.databaseBuilder(
            ctx,
            IsaiDatabase::class.java,
            IsaiDatabase.DATABASE_NAME
        ).build()

        val track = runBlocking {
            db.trackDao().getTrackById(trackId)
        } ?: throw FileNotFoundException("Track not found: $trackId")

        val artPath = track.artPath ?: throw FileNotFoundException("No artwork for track: $trackId")
        val file = File(artPath)

        if (!file.exists() || !file.canRead()) {
            throw FileNotFoundException("Artwork file inaccessible: $artPath")
        }

        // Defensive path traversal check: Ensure file is a canonical file
        val canonical = file.canonicalFile
        if (!canonical.exists() || !canonical.isFile) {
            throw SecurityException("Invalid file target: ${canonical.path}")
        }

        return ParcelFileDescriptor.open(canonical, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? = null

    override fun getType(uri: Uri): String = "image/jpeg"

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = 0
}

