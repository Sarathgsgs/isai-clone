package com.isai.player.data.scanner

import androidx.compose.runtime.Immutable

@Immutable
data class DiscoveredFile(
    val uriString: String,
    val documentId: String,
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val folderPath: String
)

sealed interface ScanState {
    data object Idle : ScanState

    data class Scanning(
        val scannedCount: Int,
        val totalDiscovered: Int,
        val currentFileName: String
    ) : ScanState {
        val progress: Float
            get() = if (totalDiscovered > 0) (scannedCount.toFloat() / totalDiscovered.toFloat()).coerceIn(0f, 1f) else 0f
    }

    data class Completed(
        val addedCount: Int,
        val updatedCount: Int,
        val missingCount: Int,
        val totalTracks: Int
    ) : ScanState

    data class Error(val message: String) : ScanState
}

