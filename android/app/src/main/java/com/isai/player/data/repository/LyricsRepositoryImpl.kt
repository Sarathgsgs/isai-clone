package com.isai.player.data.repository

import android.content.Context
import android.net.Uri
import com.isai.player.data.db.dao.LyricsDao
import com.isai.player.data.db.entity.LyricsEntity
import com.isai.player.data.lyrics.EmbeddedLyricsReader
import com.isai.player.data.lyrics.LrcParser
import com.isai.player.data.lyrics.LyricsFormat
import com.isai.player.data.lyrics.LyricsSource
import com.isai.player.data.lyrics.ParsedLyrics
import com.isai.player.data.lyrics.SidecarLyricsScanner
import com.isai.player.data.model.Track
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LyricsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val lyricsDao: LyricsDao
) : LyricsRepository {

    override fun observeLyrics(track: Track): Flow<ParsedLyrics?> = flow {
        // Emit cached or discovered lyrics
        val result = getOrDiscoverLyrics(track)
        emit(result)

        // Then react to any subsequent database updates (e.g. offset calibration or user import)
        lyricsDao.observeLyrics(track.id).collect { entity ->
            if (entity != null) {
                val parsed = LrcParser.parse(entity.content)
                emit(parsed.copy(offsetMs = entity.offsetMs))
            }
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun getOrDiscoverLyrics(track: Track): ParsedLyrics? = withContext(Dispatchers.IO) {
        // Priority 1: Room Database (holds user imports or previously cached sidecar/embedded)
        val cached = lyricsDao.getLyrics(track.id)
        if (cached != null) {
            val parsed = LrcParser.parse(cached.content)
            return@withContext parsed.copy(offsetMs = cached.offsetMs)
        }

        val trackUri = Uri.parse(track.uri)

        // Priority 2: Sidecar file (.lrc or .txt adjacent to track)
        val sidecar = SidecarLyricsScanner.findSidecar(context, trackUri)
        if (sidecar != null) {
            val format = if (sidecar.isSynced) LyricsFormat.SYNCED else LyricsFormat.PLAIN
            val entity = LyricsEntity(
                trackId = track.id,
                source = LyricsSource.SIDECAR.name,
                format = format.name,
                content = sidecar.content,
                offsetMs = 0,
                lastModified = System.currentTimeMillis()
            )
            lyricsDao.insertLyrics(entity)
            return@withContext LrcParser.parse(sidecar.content)
        }

        // Priority 3: Embedded tags (USLT / SYLT / VorbisComment)
        val embedded = EmbeddedLyricsReader.readEmbeddedLyrics(context, trackUri)
        if (!embedded.isNullOrBlank()) {
            val parsed = LrcParser.parse(embedded)
            val entity = LyricsEntity(
                trackId = track.id,
                source = LyricsSource.EMBEDDED.name,
                format = parsed.format.name,
                content = embedded,
                offsetMs = parsed.offsetMs,
                lastModified = System.currentTimeMillis()
            )
            lyricsDao.insertLyrics(entity)
            return@withContext parsed
        }

        null
    }

    override suspend fun importLyrics(trackId: Long, rawText: String) = withContext(Dispatchers.IO) {
        val parsed = LrcParser.parse(rawText)
        val entity = LyricsEntity(
            trackId = trackId,
            source = LyricsSource.IMPORTED.name,
            format = parsed.format.name,
            content = rawText,
            offsetMs = parsed.offsetMs,
            lastModified = System.currentTimeMillis()
        )
        lyricsDao.insertLyrics(entity)
    }

    override suspend fun updateLyricsOffset(trackId: Long, offsetMs: Int) = withContext(Dispatchers.IO) {
        lyricsDao.updateOffset(trackId, offsetMs)
    }

    override fun observeLyricsCount(): Flow<Int> = lyricsDao.observeLyricsCount()

    override suspend fun getLyricsCount(): Int = withContext(Dispatchers.IO) {
        lyricsDao.getLyricsCount()
    }
}

