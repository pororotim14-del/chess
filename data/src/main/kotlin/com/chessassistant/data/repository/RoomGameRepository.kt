package com.chessassistant.data.repository

import com.chessassistant.corechess.notation.FenParser
import com.chessassistant.corechess.notation.San
import com.chessassistant.corechess.model.Move
import com.chessassistant.data.local.GameEntity
import com.chessassistant.data.local.GameMoveEntity
import com.chessassistant.data.local.dao.GameDao
import com.chessassistant.data.local.dao.GameMoveDao
import com.chessassistant.domain.model.GameId
import com.chessassistant.domain.model.GameSummary
import com.chessassistant.domain.model.StoredGame
import com.chessassistant.domain.model.StoredMove
import com.chessassistant.domain.repository.GameRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Persists games through Room while computing SAN + FEN per move on save.
 */
class RoomGameRepository(
    private val gameDao: GameDao,
    private val gameMoveDao: GameMoveDao,
) : GameRepository {

    override fun observeGames(): Flow<List<GameSummary>> =
        gameDao.observeAll().map { entities ->
            entities.map { it.toSummary() }
        }

    override suspend fun saveGame(game: StoredGame): GameId {
        val movesWithMetadata = enrichMoves(game.initialFen, game.moves)
        val enriched = game.copy(moves = movesWithMetadata)
        val id = gameDao.insert(GameEntity.fromStorage(enriched))
        if (id != 0L) {
            gameMoveDao.deleteForGame(id)
            gameMoveDao.insertAll(
                movesWithMetadata.mapIndexed { i, move ->
                    GameMoveEntity(
                        gameId = id,
                        ply = i,
                        uci = move.uci,
                        san = move.san,
                        fenAfter = move.fen ?: "",
                    )
                },
            )
        }
        return GameId(id)
    }

    override suspend fun loadGame(id: GameId): StoredGame? =
        gameDao.byId(id.value)?.toStorage()

    override suspend fun deleteGame(id: GameId) {
        gameMoveDao.deleteForGame(id.value)
        gameDao.deleteById(id.value)
    }

    override suspend fun count(): Int = gameDao.count()

    private fun enrichMoves(initialFen: String, moves: List<StoredMove>): List<StoredMove> {
        var pos = FenParser.parse(initialFen) ?: return moves
        return moves.map { stored ->
            val move = Move.fromUci(stored.uci)
            val san = if (move != null) {
                San.format(pos, move).ifBlank { stored.san }
            } else {
                stored.san
            }
            val next = move?.let { pos.apply(it) }
            val fen = next?.toFen()
            if (next != null) pos = next
            stored.copy(san = san, fen = fen ?: stored.fen)
        }
    }
}