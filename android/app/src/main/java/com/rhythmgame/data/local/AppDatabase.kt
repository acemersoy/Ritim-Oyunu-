package com.rhythmgame.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.rhythmgame.data.model.Song

@Database(
    entities = [Song::class, ChartEntity::class, ProfileEntity::class, GameResultEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun chartDao(): ChartDao
    abstract fun userDao(): UserDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE songs ADD COLUMN localAudioPath TEXT")
                db.execSQL("ALTER TABLE songs ADD COLUMN errorMessage TEXT")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS charts (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        songId TEXT NOT NULL,
                        difficulty TEXT NOT NULL,
                        chartJson TEXT NOT NULL
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create profile table if not exists
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS profile (
                        id TEXT NOT NULL PRIMARY KEY,
                        username TEXT NOT NULL,
                        level INTEGER NOT NULL DEFAULT 1,
                        xp INTEGER NOT NULL DEFAULT 0,
                        league TEXT NOT NULL DEFAULT 'Bronz',
                        totalGamesPlayed INTEGER NOT NULL DEFAULT 0,
                        highestScore INTEGER NOT NULL DEFAULT 0,
                        avatarUri TEXT,
                        lastSyncTimestamp INTEGER NOT NULL DEFAULT 0,
                        isSynced INTEGER NOT NULL DEFAULT 0,
                        coins INTEGER NOT NULL DEFAULT 0,
                        energy INTEGER NOT NULL DEFAULT 120,
                        maxEnergy INTEGER NOT NULL DEFAULT 120
                    )
                """.trimIndent())

                // Create game_results table if not exists
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS game_results (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        songId TEXT NOT NULL,
                        songTitle TEXT NOT NULL,
                        score INTEGER NOT NULL,
                        accuracy REAL NOT NULL,
                        maxCombo INTEGER NOT NULL,
                        difficulty TEXT NOT NULL,
                        playedAt INTEGER NOT NULL,
                        xpEarned INTEGER NOT NULL,
                        isSynced INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())

                // Try to add currency columns to existing profile table (may already exist)
                try {
                    db.execSQL("ALTER TABLE profile ADD COLUMN coins INTEGER NOT NULL DEFAULT 0")
                } catch (_: Exception) {}
                try {
                    db.execSQL("ALTER TABLE profile ADD COLUMN energy INTEGER NOT NULL DEFAULT 120")
                } catch (_: Exception) {}
                try {
                    db.execSQL("ALTER TABLE profile ADD COLUMN maxEnergy INTEGER NOT NULL DEFAULT 120")
                } catch (_: Exception) {}
            }
        }
    }
}
