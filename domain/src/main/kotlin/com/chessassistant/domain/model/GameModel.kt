package com.chessassistant.domain.model

import kotlinx.serialization.Serializable

/** Short identifier for a stored game. */
@Serializable
data class GameId(val value: Long) {
    companion object {
        val NONE = GameId(-1)
    }
}

/** One move of a saved game, stored as UCI plus human SAN for display. */
@Serializable
data class StoredMove(
    val uci: String,
    val san: String,
    val fen: String? = null,
)

@Serializable
enum class GameOutcome {
    WHITE_WIN,
    BLACK_WIN,
    DRAW,
    ONGOING,
}

@Serializable
data class GameSummary(
    val id: GameId,
    val whiteName: String,
    val blackName: String,
    val moveCount: Int,
    val result: String,
    val outcome: GameOutcome,
    val createdAtEpochMs: Long,
)

@Serializable
data class StoredGame(
    val id: GameId,
    val initialFen: String,
    val moves: List<StoredMove>,
    val whiteName: String,
    val blackName: String,
    val result: String,
    val createdAtEpochMs: Long,
) {
    fun toSummary(): GameSummary {
        val last = moves.lastOrNull()
        val lastPos = last?.fen?.let { com.chessassistant.corechess.notation.FenParser.parse(it) }
        val status = lastPos?.let { com.chessassistant.corechess.rules.MoveGenerator.gameStatus(it) }
            ?: com.chessassistant.corechess.rules.GameStatus.NORMAL
        val outcome = when (status) {
            com.chessassistant.corechess.rules.GameStatus.CHECKMATE -> {
                if (lastPos!!.sideToMove == com.chessassistant.corechess.model.Color.WHITE) {
                    GameOutcome.BLACK_WIN
                } else {
                    GameOutcome.WHITE_WIN
                }
            }
            com.chessassistant.corechess.rules.GameStatus.STALEMATE -> GameOutcome.DRAW
            else -> GameOutcome.ONGOING
        }
        return GameSummary(
            id = id,
            whiteName = whiteName,
            blackName = blackName,
            moveCount = moves.size,
            result = result,
            outcome = outcome,
            createdAtEpochMs = createdAtEpochMs,
        )
    }
}