package com.chessassistant.app.di

import android.content.Context
import androidx.room.Room
import com.chessassistant.data.local.AppDatabase
import com.chessassistant.data.local.dao.AnalysisDao
import com.chessassistant.data.local.dao.GameDao
import com.chessassistant.data.local.dao.GameMoveDao
import com.chessassistant.data.local.dao.OpeningDao
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
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.NAME,
        ).fallbackToDestructiveMigration().build()

    @Provides
    fun provideGameDao(db: AppDatabase): GameDao = db.gameDao()

    @Provides
    fun provideGameMoveDao(db: AppDatabase): GameMoveDao = db.gameMoveDao()

    @Provides
    fun provideAnalysisDao(db: AppDatabase): AnalysisDao = db.analysisDao()

    @Provides
    fun provideOpeningDao(db: AppDatabase): OpeningDao = db.openingDao()
}