package com.chessassistant.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.chessassistant.data.local.dao.AnalysisDao
import com.chessassistant.data.local.dao.GameDao
import com.chessassistant.data.local.dao.GameMoveDao
import com.chessassistant.data.local.dao.OpeningDao

@Database(
    entities = [
        GameEntity::class,
        GameMoveEntity::class,
        AnalysisEntity::class,
        OpeningEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao
    abstract fun gameMoveDao(): GameMoveDao
    abstract fun analysisDao(): AnalysisDao
    abstract fun openingDao(): OpeningDao

    companion object {
        const val NAME = "chess-assistant.db"
    }
}