package com.chessassistant.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.chessassistant.data.local.GameMoveEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GameMoveDao {

    @Query("SELECT * FROM game_moves WHERE gameId = :gameId ORDER BY ply ASC")
    fun observeForGame(gameId: Long): Flow<List<GameMoveEntity>>

    @Insert
    suspend fun insertAll(moves: List<GameMoveEntity>)

    @Query("DELETE FROM game_moves WHERE gameId = :gameId")
    suspend fun deleteForGame(gameId: Long)

    @Query("SELECT COUNT(*) FROM game_moves WHERE gameId = :gameId")
    suspend fun countForGame(gameId: Long): Int
}