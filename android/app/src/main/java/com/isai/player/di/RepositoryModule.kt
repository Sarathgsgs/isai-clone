package com.isai.player.di

import com.isai.player.data.repository.EqualizerRepository
import com.isai.player.data.repository.EqualizerRepositoryImpl
import com.isai.player.data.repository.FavoritesRepository
import com.isai.player.data.repository.FavoritesRepositoryImpl
import com.isai.player.data.repository.HistoryRepository
import com.isai.player.data.repository.HistoryRepositoryImpl
import com.isai.player.data.repository.LibraryRepository
import com.isai.player.data.repository.LibraryRepositoryImpl
import com.isai.player.data.repository.PlaylistRepository
import com.isai.player.data.repository.PlaylistRepositoryImpl
import com.isai.player.data.repository.QueueRepository
import com.isai.player.data.repository.QueueRepositoryImpl
import com.isai.player.data.repository.SettingsRepository
import com.isai.player.data.repository.SettingsRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        impl: SettingsRepositoryImpl
    ): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindLibraryRepository(
        impl: LibraryRepositoryImpl
    ): LibraryRepository

    @Binds
    @Singleton
    abstract fun bindFavoritesRepository(
        impl: FavoritesRepositoryImpl
    ): FavoritesRepository

    @Binds
    @Singleton
    abstract fun bindPlaylistRepository(
        impl: PlaylistRepositoryImpl
    ): PlaylistRepository

    @Binds
    @Singleton
    abstract fun bindHistoryRepository(
        impl: HistoryRepositoryImpl
    ): HistoryRepository

    @Binds
    @Singleton
    abstract fun bindQueueRepository(
        impl: QueueRepositoryImpl
    ): QueueRepository

    @Binds
    @Singleton
    abstract fun bindEqualizerRepository(
        impl: EqualizerRepositoryImpl
    ): EqualizerRepository
}

