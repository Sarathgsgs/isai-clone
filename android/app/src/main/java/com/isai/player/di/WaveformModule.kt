package com.isai.player.di

import com.isai.player.data.repository.WaveformRepository
import com.isai.player.data.repository.WaveformRepositoryImpl
import com.isai.player.data.waveform.WaveformExtractor
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class WaveformModule {

    @Binds
    @Singleton
    abstract fun bindWaveformRepository(
        impl: WaveformRepositoryImpl
    ): WaveformRepository

    companion object {
        @Provides
        @Singleton
        fun provideWaveformExtractor(): WaveformExtractor = WaveformExtractor()
    }
}

