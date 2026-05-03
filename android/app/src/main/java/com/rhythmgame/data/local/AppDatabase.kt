package com.rhythmgame.data.local

import android.database.Cursor
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.rhythmgame.data.model.Song

@Database(
    entities = [Song::class, ChartEntity::class, ProfileEntity::class, GameResultEntity::class, AchievementEntity::class],
    version = 6,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun chartDao(): ChartDao
    abstract fun userDao(): UserDao
    abstract fun achievementDao(): AchievementDao

    companion object {

        private fun SupportSQLiteDatabase.hasColumn(table: String, column: String): Boolean {
            val cursor: Cursor = query("PRAGMA table_info($table)")
            cursor.use {
                val nameIndex = it.getColumnIndex("name")
                while (it.moveToNext()) {
                    if (it.getString(nameIndex) == column) return true
                }
            }
            return false
        }

        private fun SupportSQLiteDatabase.addColumnIfMissing(
            table: String,
            column: String,
            type: String,
            defaultValue: String,
        ) {
            if (!hasColumn(table, column)) {
                execSQL("ALTER TABLE $table ADD COLUMN $column $type NOT NULL DEFAULT $defaultValue")
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.addColumnIfMissing("songs", "localAudioPath", "TEXT", "''")
                db.addColumnIfMissing("songs", "errorMessage", "TEXT", "''")

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

                db.addColumnIfMissing("profile", "coins", "INTEGER", "0")
                db.addColumnIfMissing("profile", "energy", "INTEGER", "120")
                db.addColumnIfMissing("profile", "maxEnergy", "INTEGER", "120")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.addColumnIfMissing("songs", "isFavorite", "INTEGER", "0")
                db.addColumnIfMissing("songs", "lastPlayedAt", "INTEGER", "0")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.addColumnIfMissing("profile", "stars", "INTEGER", "0")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS achievements (
                        id TEXT NOT NULL PRIMARY KEY,
                        title TEXT NOT NULL,
                        description TEXT NOT NULL,
                        category TEXT NOT NULL,
                        isUnlocked INTEGER NOT NULL DEFAULT 0,
                        unlockedAt INTEGER NOT NULL DEFAULT 0,
                        progress INTEGER NOT NULL DEFAULT 0,
                        target INTEGER NOT NULL DEFAULT 1
                    )
                """.trimIndent())
            }
        }
    }
}
