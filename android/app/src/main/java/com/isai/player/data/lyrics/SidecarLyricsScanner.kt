package com.isai.player.data.lyrics

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.nio.charset.StandardCharsets

data class SidecarLyrics(
    val uri: Uri,
    val content: String,
    val isSynced: Boolean
)

object SidecarLyricsScanner {

    /**
     * Looks for an adjacent .lrc or .txt file matching the audio track's base name.
     * Priority: .lrc (synced) > .txt (plain)
     */
    fun findSidecar(context: Context, trackUri: Uri): SidecarLyrics? {
        val scheme = trackUri.scheme ?: ""

        // 1. Direct file:// or absolute path
        if (scheme == "file" || scheme.isEmpty()) {
            val file = File(trackUri.path ?: return null)
            val parent = file.parentFile ?: return null
            val baseName = file.nameWithoutExtension

            val lrcFile = File(parent, "$baseName.lrc")
            if (lrcFile.exists() && lrcFile.canRead()) {
                val content = lrcFile.readText(StandardCharsets.UTF_8)
                return SidecarLyrics(Uri.fromFile(lrcFile), content, isSynced = true)
            }

            val txtFile = File(parent, "$baseName.txt")
            if (txtFile.exists() && txtFile.canRead()) {
                val content = txtFile.readText(StandardCharsets.UTF_8)
                return SidecarLyrics(Uri.fromFile(txtFile), content, isSynced = false)
            }
        }

        // 2. Storage Access Framework (content://)
        if (scheme == "content") {
            try {
                val doc = DocumentFile.fromSingleUri(context, trackUri) ?: return null
                val parentDoc = doc.parentFile
                val fileName = doc.name ?: return null
                val baseName = fileName.substringBeforeLast('.')

                if (parentDoc != null) {
                    val lrcDoc = parentDoc.findFile("$baseName.lrc")
                    if (lrcDoc != null && lrcDoc.exists()) {
                        context.contentResolver.openInputStream(lrcDoc.uri)?.use { stream ->
                            val text = stream.bufferedReader(StandardCharsets.UTF_8).readText()
                            return SidecarLyrics(lrcDoc.uri, text, isSynced = true)
                        }
                    }

                    val txtDoc = parentDoc.findFile("$baseName.txt")
                    if (txtDoc != null && txtDoc.exists()) {
                        context.contentResolver.openInputStream(txtDoc.uri)?.use { stream ->
                            val text = stream.bufferedReader(StandardCharsets.UTF_8).readText()
                            return SidecarLyrics(txtDoc.uri, text, isSynced = false)
                        }
                    }
                }
            } catch (_: Exception) {
                // Return null if SAF permissions expired or document tree unreadable
            }
        }

        return null
    }
}

