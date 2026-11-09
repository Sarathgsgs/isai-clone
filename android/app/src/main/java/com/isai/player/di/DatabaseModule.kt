package com.isai.player.di

import android.content.Context
import androidx.room.Room
import com.isai.player.data.db.IsaiDatabase
import com.isai.player.data.db.dao.AlbumDao
import com.isai.player.data.db.dao.ArtistDao
import com.isai.player.data.db.dao.FavoriteDao
import com.isai.player.data.db.dao.FolderDao
import com.isai.player.data.db.dao.HistoryDao
import com.isai.player.data.db.dao.LyricsDao
import com.isai.player.data.db.dao.PlaylistDao
import com.isai.player.data.db.dao.QueueDao
import com.isai.player.data.db.dao.TrackDao
import com.isai.player.data.db.dao.WaveformDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideIsaiDatabase(
        @ApplicationContext context: Context
    ): IsaiDatabase {
        return Room.databaseBuilder(
            context,
            IsaiDatabase::class.java,
            IsaiDatabase.DATABASE_NAME
        )
            .addMigrations(
                IsaiDatabase.MIGRATION_1_2,
                IsaiDatabase.MIGRATION_2_3,
                IsaiDatabase.MIGRATION_3_4
            )
            .build()
    }

    @Provides
    fun provideTrackDao(database: IsaiDatabase): TrackDao = database.trackDao()

    @Provides
    fun provideAlbumDao(database: IsaiDatabase): AlbumDao = database.albumDao()

    @Provides
    fun provideArtistDao(database: IsaiDatabase): ArtistDao = database.artistDao()

    @Provides
    fun provideFolderDao(database: IsaiDatabase): FolderDao = database.folderDao()

    @Provides
    fun provideFavoriteDao(database: IsaiDatabase): FavoriteDao = database.favoriteDao()

    @Provides
    fun providePlaylistDao(database: IsaiDatabase): PlaylistDao = database.playlistDao()

    @Provides
    fun provideHistoryDao(database: IsaiDatabase): HistoryDao = database.historyDao()

    @Provides
    fun provideQueueDao(database: IsaiDatabase): QueueDao = database.queueDao()

    @Provides
    fun provideWaveformDao(database: IsaiDatabase): WaveformDao = database.waveformDao()

    @Provides
    fun provideLyricsDao(database: IsaiDatabase): LyricsDao = database.lyricsDao()
}
