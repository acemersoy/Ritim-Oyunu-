package com.rhythmgame.di

import android.content.Context
import androidx.room.Room
import com.rhythmgame.data.local.AppDatabase
import com.rhythmgame.data.local.ChartDao
import com.rhythmgame.data.local.SongDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "rhythm_game_db"
        )
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()
    }

    @Provides
    @Singleton
    fun provideSongDao(database: AppDatabase): SongDao = database.songDao()

    @Provides
    @Singleton
    fun provideChartDao(database: AppDatabase): ChartDao = database.chartDao()
}
