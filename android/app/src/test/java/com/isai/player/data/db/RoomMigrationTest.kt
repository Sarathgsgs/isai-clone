package com.isai.player.data.db

import androidx.sqlite.db.SupportSQLiteDatabase
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test

class RoomMigrationTest {

    @Test
    fun migration_1_2_hasCorrectVersionsAndExecutesExpectedDDL() {
        val migration = IsaiDatabase.MIGRATION_1_2
        assertEquals(1, migration.startVersion)
        assertEquals(2, migration.endVersion)

        val db = mockk<SupportSQLiteDatabase>(relaxed = true)
        migration.migrate(db)

        verify(exactly = 1) {
            db.execSQL(match { it.contains("CREATE TABLE IF NOT EXISTS `waveforms`") && it.contains("`track_id` INTEGER NOT NULL") })
        }
    }

    @Test
    fun migration_2_3_hasCorrectVersionsAndExecutesExpectedDDL() {
        val migration = IsaiDatabase.MIGRATION_2_3
        assertEquals(2, migration.startVersion)
        assertEquals(3, migration.endVersion)

        val db = mockk<SupportSQLiteDatabase>(relaxed = true)
        migration.migrate(db)

        verify(exactly = 1) {
            db.execSQL(match { it.contains("CREATE TABLE IF NOT EXISTS `lyrics`") && it.contains("`offset_ms` INTEGER NOT NULL") })
        }
    }

    @Test
    fun migration_3_4_hasCorrectVersionsAndExecutesExpectedDDL() {
        val migration = IsaiDatabase.MIGRATION_3_4
        assertEquals(3, migration.startVersion)
        assertEquals(4, migration.endVersion)

        val db = mockk<SupportSQLiteDatabase>(relaxed = true)
        migration.migrate(db)

        verify(exactly = 1) {
            db.execSQL(match { it.contains("CREATE INDEX IF NOT EXISTS `index_tracks_relative_path_norm`") })
        }
    }
}

