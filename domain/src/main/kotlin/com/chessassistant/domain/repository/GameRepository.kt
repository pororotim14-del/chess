package com.chessassistant.domain.repository

import com.chessassistant.domain.model.GameId
import com.chessassistant.domain.model.GameSummary
import com.chessassistant.domain.model.StoredGame
import kotlinx.coroutines.flow.Flow

/**
 * Storage for played games. Implementations live in the data module.
 */
interface GameRepository {

    fun observeGames(): Flow<List<GameSummary>>

    suspend fun saveGame(game: StoredGame): GameId

    suspend fun loadGame(id: GameId): StoredGame?

    suspend fun deleteGame(id: GameId)

    suspend fun count(): Int
}