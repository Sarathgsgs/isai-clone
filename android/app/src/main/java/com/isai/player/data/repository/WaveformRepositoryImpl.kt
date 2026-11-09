package com.isai.player.data.repository

import android.content.Context
import android.net.Uri
import com.isai.player.data.db.dao.WaveformDao
import com.isai.player.data.db.entity.WaveformEntity
import com.isai.player.data.waveform.WaveformExtractor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WaveformRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val waveformDao: WaveformDao,
    private val waveformExtractor: WaveformExtractor
) : WaveformRepository {

    companion object {
        const val CURRENT_WAVEFORM_VERSION = 1
    }

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val inFlightJobs = ConcurrentHashMap<Long, Deferred<ByteArray?>>()

    override fun observeWaveform(
        trackId: Long,
        durationMs: Long,
        trackUri: Uri,
        lastModified: Long
    ): Flow<WaveformState> = flow {
        emit(WaveformState.Loading)

        if (durationMs <= 0) {
            emit(WaveformState.Unavailable)
            return@flow
        }

        // 1. Check local Room database cache
        val cached = waveformDao.getWaveform(trackId)
        if (cached != null) {
            // Check version validity and file edit timestamp
            if (cached.version == CURRENT_WAVEFORM_VERSION && cached.createdAt >= lastModified) {
                emit(WaveformState.Ready(cached.peaks))
                return@flow
            } else {
                waveformDao.deleteWaveform(trackId)
            }
        }

        // 2. Decode or await already in-flight decoding job
        val deferred = inFlightJobs.computeIfAbsent(trackId) {
            repositoryScope.async {
                val extracted = waveformExtractor.extract(
                    context = context,
                    trackUri = trackUri,
                    durationUs = durationMs * 1000L
                )
                if (extracted != null && extracted.isNotEmpty()) {
                    val entity = WaveformEntity(
                        trackId = trackId,
                        bucketCount = extracted.size,
                        peaks = extracted,
                        version = CURRENT_WAVEFORM_VERSION,
                        createdAt = System.currentTimeMillis()
                    )
                    waveformDao.insertWaveform(entity)
                    extracted
                } else {
                    null
                }
            }
        }

        try {
            val result = deferred.await()
            if (result != null) {
                emit(WaveformState.Ready(result))
            } else {
                emit(WaveformState.Unavailable)
            }
        } finally {
            inFlightJobs.remove(trackId)
        }
    }

    override fun prefetchWaveform(
        trackId: Long,
        durationMs: Long,
        trackUri: Uri,
        lastModified: Long
    ) {
        if (durationMs <= 0 || inFlightJobs.containsKey(trackId)) return

        repositoryScope.launch {
            val cached = waveformDao.getWaveform(trackId)
            if (cached != null && cached.version == CURRENT_WAVEFORM_VERSION && cached.createdAt >= lastModified) {
                return@launch
            }

            val deferred = inFlightJobs.computeIfAbsent(trackId) {
                repositoryScope.async {
                    val extracted = waveformExtractor.extract(
                        context = context,
                        trackUri = trackUri,
                        durationUs = durationMs * 1000L
                    )
                    if (extracted != null && extracted.isNotEmpty()) {
                        waveformDao.insertWaveform(
                            WaveformEntity(
                                trackId = trackId,
                                bucketCount = extracted.size,
                                peaks = extracted,
                                version = CURRENT_WAVEFORM_VERSION,
                                createdAt = System.currentTimeMillis()
                            )
                        )
                        extracted
                    } else {
                        null
                    }
                }
            }
            try {
                deferred.await()
            } finally {
                inFlightJobs.remove(trackId)
            }
        }
    }

    override fun observeWaveformCount(): Flow<Int> = waveformDao.observeWaveformCount()

    override suspend fun getWaveformCount(): Int = waveformDao.getWaveformCount()

    override suspend fun clearWaveformCache() {
        waveformDao.clearAllWaveforms()
    }
}

