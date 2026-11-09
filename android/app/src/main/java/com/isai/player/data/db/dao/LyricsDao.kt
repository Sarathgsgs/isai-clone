package com.isai.player.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.isai.player.data.db.entity.LyricsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LyricsDao {

    @Query("SELECT * FROM lyrics WHERE track_id = :trackId LIMIT 1")
    suspend fun getLyrics(trackId: Long): LyricsEntity?

    @Query("SELECT * FROM lyrics WHERE track_id = :trackId LIMIT 1")
    fun observeLyrics(trackId: Long): Flow<LyricsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLyrics(lyrics: LyricsEntity)

    @Query("UPDATE lyrics SET offset_ms = :offsetMs, last_modified = :now WHERE track_id = :trackId")
    suspend fun updateOffset(trackId: Long, offsetMs: Int, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM lyrics WHERE track_id = :trackId")
    suspend fun deleteLyrics(trackId: Long)

    @Query("SELECT COUNT(*) FROM lyrics")
    fun observeLyricsCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM lyrics")
    suspend fun getLyricsCount(): Int
}

