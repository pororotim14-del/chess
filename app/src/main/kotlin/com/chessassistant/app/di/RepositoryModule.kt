package com.chessassistant.app.di

import com.chessassistant.coreengine.analysis.AnalysisEngine
import com.chessassistant.coreengine.analysis.DefaultAnalysisEngine
import com.chessassistant.coreengine.trackers.DefaultOpeningBook
import com.chessassistant.coreengine.trackers.OpeningBook
import com.chessassistant.data.repository.DataStorePreferencesRepository
import com.chessassistant.data.repository.DefaultAnalysisRepository
import com.chessassistant.data.repository.DefaultOpeningRepository
import com.chessassistant.data.repository.RoomGameRepository
import com.chessassistant.data.local.dao.AnalysisDao
import com.chessassistant.data.local.dao.GameDao
import com.chessassistant.data.local.dao.GameMoveDao
import com.chessassistant.domain.repository.AnalysisRepository
import com.chessassistant.domain.repository.GameRepository
import com.chessassistant.domain.repository.OpeningRepository
import com.chessassistant.domain.repository.PreferencesRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideAnalysisEngine(): AnalysisEngine = DefaultAnalysisEngine()

    @Provides
    @Singleton
    fun provideOpeningBook(): OpeningBook = DefaultOpeningBook()

    @Provides
    fun provideAppScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    @Provides
    @Singleton
    fun provideGameRepository(
        gameDao: GameDao,
        gameMoveDao: GameMoveDao,
    ): GameRepository = RoomGameRepository(gameDao, gameMoveDao)

    @Provides
    @Singleton
    fun provideAnalysisRepository(
        engine: AnalysisEngine,
        scope: CoroutineScope,
        dispatcher: CoroutineDispatcher,
    ): AnalysisRepository = DefaultAnalysisRepository(engine, scope, dispatcher)

    @Provides
    @Singleton
    fun provideOpeningRepository(
        book: OpeningBook,
    ): OpeningRepository = DefaultOpeningRepository(book)

    @Provides
    @Singleton
    fun providePreferencesRepository(
        @ApplicationContext context: android.content.Context,
    ): PreferencesRepository = DataStorePreferencesRepository(context)
}