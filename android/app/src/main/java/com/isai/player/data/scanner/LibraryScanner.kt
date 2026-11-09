package com.isai.player.data.scanner

import android.net.Uri
import com.isai.player.data.db.dao.TrackDao
import com.isai.player.data.db.entity.TrackEntity
import com.isai.player.data.repository.SettingsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LibraryScanner @Inject constructor(
    private val folderScanner: FolderScanner,
    private val metadataExtractor: MetadataExtractor,
    private val trackDao: TrackDao,
    private val settingsRepository: SettingsRepository
) {
    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    private val scanMutex = Mutex()
    private val workerDispatcher: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(4)

    suspend fun startScan(treeUri: Uri): ScanState = scanMutex.withLock {
        _scanState.value = ScanState.Scanning(scannedCount = 0, totalDiscovered = 0, currentFileName = "")

        try {
            // 1. Fetch existing tracks for incremental diffing
            val existingTracks = withContext(Dispatchers.IO) {
                trackDao.getAllTrackUriAndLastModified()
            }
            val existingMap = existingTracks.associate { it.uri to it.lastModified }

            // 2. Scan document tree using high-speed DocumentsContract queries
            val discoveredFiles = folderScanner.scanTree(treeUri)
            val discoveredUris = discoveredFiles.map { it.uriString }.toSet()

            // 3. Diff files: unmodified vs (new or modified)
            val filesToExtract = mutableListOf<DiscoveredFile>()
            var unmodifiedCount = 0

            for (file in discoveredFiles) {
                val existingModified = existingMap[file.uriString]
                if (existingModified != null && existingModified == file.lastModified) {
                    unmodifiedCount++
                } else {
                    filesToExtract.add(file)
                }
            }

            val totalDiscovered = discoveredFiles.size
            val processedCounter = AtomicInteger(unmodifiedCount)

            _scanState.value = ScanState.Scanning(
                scannedCount = unmodifiedCount,
                totalDiscovered = totalDiscovered,
                currentFileName = if (filesToExtract.isNotEmpty()) filesToExtract.first().displayName else ""
            )

            // 4. Extract metadata for new/modified files in parallel batches
            var addedCount = 0
            var updatedCount = 0
            val batchSize = 25
            val extractedBatch = mutableListOf<TrackEntity>()

            // Chunk files for bounded concurrency
            for (chunk in filesToExtract.chunked(8)) {
                val extractedList = withContext(workerDispatcher) {
                    chunk.map { file ->
                        async {
                            val track = metadataExtractor.extractTrack(file)
                            val count = processedCounter.incrementAndGet()
                            _scanState.value = ScanState.Scanning(
                                scannedCount = count,
                                totalDiscovered = totalDiscovered,
                                currentFileName = file.displayName
                            )
                            track
                        }
                    }.awaitAll()
                }

                for (track in extractedList) {
                    if (existingMap.containsKey(track.uri)) {
                        updatedCount++
                    } else {
                        addedCount++
                    }
                    extractedBatch.add(track)

                    if (extractedBatch.size >= batchSize) {
                        withContext(Dispatchers.IO) {
                            trackDao.insertTracks(extractedBatch)
                        }
                        extractedBatch.clear()
                    }
                }
            }

            if (extractedBatch.isNotEmpty()) {
                withContext(Dispatchers.IO) {
                    trackDao.insertTracks(extractedBatch)
                }
                extractedBatch.clear()
            }

            // 5. Identify deleted or missing tracks
            val missingUris = existingMap.keys.filterNot { it in discoveredUris }
            var missingCount = 0
            if (missingUris.isNotEmpty()) {
                withContext(Dispatchers.IO) {
                    trackDao.markTracksMissing(missingUris)
                }
                missingCount = missingUris.size
            }

            val totalTracks = withContext(Dispatchers.IO) {
                trackDao.observeTrackCount().first()
            }

            val completed = ScanState.Completed(
                addedCount = addedCount,
                updatedCount = updatedCount,
                missingCount = missingCount,
                totalTracks = totalTracks
            )
            _scanState.value = completed
            return completed
        } catch (e: Exception) {
            val error = ScanState.Error(e.message ?: "Failed to scan folder")
            _scanState.value = error
            return error
        }
    }

    fun resetState() {
        _scanState.value = ScanState.Idle
    }
}

