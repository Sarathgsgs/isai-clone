package com.isai.player.data.repository

import android.content.Context
import android.net.Uri
import com.isai.player.data.db.dao.WaveformDao
import com.isai.player.data.db.entity.WaveformEntity
import com.isai.player.data.waveform.WaveformExtractor
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WaveformRepositoryTest {

    private val context: Context = mockk(relaxed = true)
    private val waveformDao: WaveformDao = mockk(relaxed = true)
    private val waveformExtractor: WaveformExtractor = mockk(relaxed = true)
    private lateinit var repository: WaveformRepositoryImpl

    private val testUri = mockk<Uri>(relaxed = true)

    @Before
    fun setUp() {
        repository = WaveformRepositoryImpl(context, waveformDao, waveformExtractor)
    }

    @Test
    fun cacheHitAvoidsDecoding() = runTest {
        val trackId = 101L
        val cachedPeaks = ByteArray(200) { 120.toByte() }
        val cachedEntity = WaveformEntity(
            trackId = trackId,
            bucketCount = 200,
            peaks = cachedPeaks,
            version = WaveformRepositoryImpl.CURRENT_WAVEFORM_VERSION,
            createdAt = 1000L
        )

        coEvery { waveformDao.getWaveform(trackId) } returns cachedEntity

        val states = mutableListOf<WaveformState>()
        repository.observeWaveform(trackId, 180000L, testUri, lastModified = 500L).collect {
            states.add(it)
        }

        assertTrue("First state must be Loading", states[0] is WaveformState.Loading)
        assertTrue("Second state must be Ready", states[1] is WaveformState.Ready)
        val ready = states[1] as WaveformState.Ready
        assertTrue(ready.peaks.contentEquals(cachedPeaks))

        // Decoder was never invoked
        coVerify(exactly = 0) { waveformExtractor.extract(any(), any(), any(), any()) }
    }

    @Test
    fun staleVersionInvalidatesCacheAndTriggersNewDecode() = runTest {
        val trackId = 102L
        val staleEntity = WaveformEntity(
            trackId = trackId,
            bucketCount = 200,
            peaks = ByteArray(200) { 50.toByte() },
            version = 0, // Old version
            createdAt = 1000L
        )
        val freshPeaks = ByteArray(200) { 200.toByte() }

        coEvery { waveformDao.getWaveform(trackId) } returns staleEntity
        coEvery { waveformExtractor.extract(any(), any(), any(), any()) } returns freshPeaks

        val states = mutableListOf<WaveformState>()
        repository.observeWaveform(trackId, 180000L, testUri, lastModified = 500L).collect {
            states.add(it)
        }

        // Stale entry must be deleted
        coVerify(exactly = 1) { waveformDao.deleteWaveform(trackId) }
        // New extraction must be called
        coVerify(exactly = 1) { waveformExtractor.extract(any(), any(), any(), any()) }
        // New entity inserted
        coVerify(exactly = 1) { waveformDao.insertWaveform(match { it.trackId == trackId && it.version == WaveformRepositoryImpl.CURRENT_WAVEFORM_VERSION }) }

        assertTrue(states.last() is WaveformState.Ready)
        val ready = states.last() as WaveformState.Ready
        assertTrue(ready.peaks.contentEquals(freshPeaks))
    }

    @Test
    fun modifiedFileInvalidatesCache() = runTest {
        val trackId = 103L
        val cachedEntity = WaveformEntity(
            trackId = trackId,
            bucketCount = 200,
            peaks = ByteArray(200) { 80.toByte() },
            version = WaveformRepositoryImpl.CURRENT_WAVEFORM_VERSION,
            createdAt = 1000L // Cache created at 1000L
        )
        val freshPeaks = ByteArray(200) { 150.toByte() }

        coEvery { waveformDao.getWaveform(trackId) } returns cachedEntity
        coEvery { waveformExtractor.extract(any(), any(), any(), any()) } returns freshPeaks

        // File modified at 2000L > 1000L
        val states = mutableListOf<WaveformState>()
        repository.observeWaveform(trackId, 180000L, testUri, lastModified = 2000L).collect {
            states.add(it)
        }

        coVerify(exactly = 1) { waveformDao.deleteWaveform(trackId) }
        coVerify(exactly = 1) { waveformExtractor.extract(any(), any(), any(), any()) }
        assertTrue(states.last() is WaveformState.Ready)
    }

    @Test
    fun zeroDurationEmitsUnavailableImmediately() = runTest {
        val states = mutableListOf<WaveformState>()
        repository.observeWaveform(104L, 0L, testUri, 0L).collect {
            states.add(it)
        }

        assertEquals(2, states.size)
        assertTrue(states[0] is WaveformState.Loading)
        assertTrue(states[1] is WaveformState.Unavailable)
        coVerify(exactly = 0) { waveformExtractor.extract(any(), any(), any(), any()) }
    }
}

