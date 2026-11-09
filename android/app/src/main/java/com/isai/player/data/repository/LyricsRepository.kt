package com.isai.player.data.repository

import com.isai.player.data.lyrics.LyricsLine
import com.isai.player.data.lyrics.ParsedLyrics
import com.isai.player.data.model.Track
import kotlinx.coroutines.flow.Flow

sealed interface LyricsState {
    object Loading : LyricsState
    data class Synced(val lines: List<LyricsLine>, val activeIndex: Int, val offsetMs: Int) : LyricsState
    data class Plain(val lines: List<String>) : LyricsState
    object Empty : LyricsState
}

interface LyricsRepository {
    fun observeLyrics(track: Track): Flow<ParsedLyrics?>

    suspend fun getOrDiscoverLyrics(track: Track): ParsedLyrics?

    suspend fun importLyrics(trackId: Long, rawText: String)

    suspend fun updateLyricsOffset(trackId: Long, offsetMs: Int)

    fun observeLyricsCount(): Flow<Int>

    suspend fun getLyricsCount(): Int
}

