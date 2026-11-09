package com.isai.player.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
import com.isai.player.data.db.entity.FavoriteEntity
import com.isai.player.data.db.entity.LyricsEntity
import com.isai.player.data.db.entity.PlayHistoryEntity
import com.isai.player.data.db.entity.PlaylistEntity
import com.isai.player.data.db.entity.PlaylistTrackEntity
import com.isai.player.data.db.entity.QueueItemEntity
import com.isai.player.data.db.entity.TrackEntity
import com.isai.player.data.db.entity.WaveformEntity

@Database(
    entities = [
        TrackEntity::class,
        PlaylistEntity::class,
        PlaylistTrackEntity::class,
        FavoriteEntity::class,
        PlayHistoryEntity::class,
        QueueItemEntity::class,
        WaveformEntity::class,
        LyricsEntity::class
    ],
    version = 4,
    exportSchema = true
)
abstract class IsaiDatabase : RoomDatabase() {

    abstract fun trackDao(): TrackDao
    abstract fun albumDao(): AlbumDao
    abstract fun artistDao(): ArtistDao
    abstract fun folderDao(): FolderDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun historyDao(): HistoryDao
    abstract fun queueDao(): QueueDao
    abstract fun waveformDao(): WaveformDao
    abstract fun lyricsDao(): LyricsDao

    companion object {
        const val DATABASE_NAME = "isai_player.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `waveforms` (
                        `track_id` INTEGER NOT NULL,
                        `bucket_count` INTEGER NOT NULL,
                        `peaks` BLOB NOT NULL,
                        `version` INTEGER NOT NULL,
                        `created_at` INTEGER NOT NULL,
                        PRIMARY KEY(`track_id`),
                        FOREIGN KEY(`track_id`) REFERENCES `tracks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `lyrics` (
                        `track_id` INTEGER NOT NULL,
                        `source` TEXT NOT NULL,
                        `format` TEXT NOT NULL,
                        `content` TEXT NOT NULL,
                        `offset_ms` INTEGER NOT NULL DEFAULT 0,
                        `last_modified` INTEGER NOT NULL,
                        PRIMARY KEY(`track_id`),
                        FOREIGN KEY(`track_id`) REFERENCES `tracks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_tracks_relative_path_norm` ON `tracks` (`relative_path_norm`)"
                )
            }
        }
    }
}
