package com.isai.player.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.isai.player.data.db.entity.WaveformEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WaveformDao {

    @Query("SELECT * FROM waveforms WHERE track_id = :trackId LIMIT 1")
    suspend fun getWaveform(trackId: Long): WaveformEntity?

    @Query("SELECT * FROM waveforms WHERE track_id = :trackId LIMIT 1")
    fun observeWaveform(trackId: Long): Flow<WaveformEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaveform(waveform: WaveformEntity)

    @Query("DELETE FROM waveforms WHERE track_id = :trackId")
    suspend fun deleteWaveform(trackId: Long)

    @Query("DELETE FROM waveforms")
    suspend fun clearAllWaveforms()

    @Query("SELECT COUNT(*) FROM waveforms")
    fun observeWaveformCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM waveforms")
    suspend fun getWaveformCount(): Int
}

