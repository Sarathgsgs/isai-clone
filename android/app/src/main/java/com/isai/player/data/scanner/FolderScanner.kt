package com.isai.player.data.scanner

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.util.ArrayDeque
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FolderScanner @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val audioExtensions = setOf(
        "mp3", "flac", "wav", "m4a", "aac", "ogg", "opus", "wma", "aiff"
    )

    private val projection = arrayOf(
        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
        DocumentsContract.Document.COLUMN_MIME_TYPE,
        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
        DocumentsContract.Document.COLUMN_LAST_MODIFIED,
        DocumentsContract.Document.COLUMN_SIZE
    )

    /**
     * Recursively traverses a SAF document tree starting at [treeUri] using direct
     * [DocumentsContract] child queries.
     *
     * @param onFileDiscovered Callback invoked each time an audio file is discovered.
     * @return List of all discovered audio files.
     */
    suspend fun scanTree(
        treeUri: Uri,
        onFileDiscovered: ((DiscoveredFile) -> Unit)? = null
    ): List<DiscoveredFile> {
        val contentResolver = context.contentResolver
        val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
        val discoveredFiles = mutableListOf<DiscoveredFile>()

        data class FolderQueueItem(val docId: String, val relativePath: String)
        val queue = ArrayDeque<FolderQueueItem>()
        queue.add(FolderQueueItem(rootDocId, ""))

        while (queue.isNotEmpty()) {
            currentCoroutineContext().ensureActive()
            val (currentDocId, parentPath) = queue.removeFirst()

            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, currentDocId)

            try {
                contentResolver.query(
                    childrenUri,
                    projection,
                    null,
                    null,
                    null
                )?.use { cursor ->
                    val idCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                    val mimeCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                    val nameCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    val modCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                    val sizeCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)

                    while (cursor.moveToNext()) {
                        currentCoroutineContext().ensureActive()

                        val docId = if (idCol >= 0) cursor.getString(idCol) ?: continue else continue
                        val mimeType = if (mimeCol >= 0) cursor.getString(mimeCol) ?: "" else ""
                        val displayName = if (nameCol >= 0) cursor.getString(nameCol) ?: "Unknown" else "Unknown"
                        val lastModified = if (modCol >= 0) cursor.getLong(modCol) else 0L
                        val sizeBytes = if (sizeCol >= 0) cursor.getLong(sizeCol) else 0L

                        if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                            val nextPath = if (parentPath.isEmpty()) displayName else "$parentPath/$displayName"
                            queue.add(FolderQueueItem(docId, nextPath))
                        } else if (isAudioFile(mimeType, displayName)) {
                            val documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                            val file = DiscoveredFile(
                                uriString = documentUri.toString(),
                                documentId = docId,
                                displayName = displayName,
                                mimeType = mimeType.ifBlank { guessMimeType(displayName) },
                                sizeBytes = sizeBytes,
                                lastModified = lastModified,
                                folderPath = if (parentPath.isEmpty()) "Root" else parentPath
                            )
                            discoveredFiles.add(file)
                            onFileDiscovered?.invoke(file)
                        }
                    }
                }
            } catch (e: Exception) {
                // Safeguard against inaccessible or permission-restricted subdirectories
                continue
            }
        }

        return discoveredFiles
    }

    fun isAudioFile(mimeType: String?, displayName: String?): Boolean {
        if (mimeType != null && mimeType.startsWith("audio/", ignoreCase = true)) {
            return true
        }
        val extension = displayName?.substringAfterLast('.', "")?.lowercase(Locale.ROOT) ?: ""
        return extension in audioExtensions
    }

    private fun guessMimeType(displayName: String): String {
        val ext = displayName.substringAfterLast('.', "").lowercase(Locale.ROOT)
        return when (ext) {
            "mp3" -> "audio/mpeg"
            "flac" -> "audio/flac"
            "wav" -> "audio/wav"
            "m4a", "aac" -> "audio/mp4"
            "ogg" -> "audio/ogg"
            "opus" -> "audio/opus"
            else -> "audio/*"
        }
    }
}

