package com.rhythmgame.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.rhythmgame.data.model.Song

@Database(entities = [Song::class, ChartEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun chartDao(): ChartDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add new columns to songs table
                db.execSQL("ALTER TABLE songs ADD COLUMN localAudioPath TEXT")
                db.execSQL("ALTER TABLE songs ADD COLUMN errorMessage TEXT")

                // Create charts table
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
    }
}
