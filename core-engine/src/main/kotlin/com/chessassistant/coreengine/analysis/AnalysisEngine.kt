package com.chessassistant.coreengine.analysis

import com.chessassistant.corechess.model.Position
import kotlinx.coroutines.flow.StateFlow

/**
 * A single engine evaluation, relative to the side to move.
 */
data class EngineScore(
    val centipawns: Int,
    val mateIn: Int? = null,
) {
    val isMate: Boolean get() = mateIn != null

    companion object {
        val DRAW = EngineScore(0)
        val WIN = EngineScore(100_000)
        val LOSS = EngineScore(-100_000)
    }
}

/**
 * One principal variation: a sequence of moves plus the score of the line.
 */
data class EngineLine(
    val moves: List<String>,
    val score: EngineScore,
) {
    val firstMove: String get() = moves.firstOrNull().orEmpty()
}

/**
 * Result of a completed search for the best move in a position.
 */
data class EngineBest(
    val move: String,
    val pv: List<String> = emptyList(),
    val score: EngineScore = EngineScore.DRAW,
) {
    companion object {
        val NONE = EngineBest("")
    }
}

/**
 * Device-side chess analysis backend.
 *
 * Implementations are one-shot and should be [dispose]d when the caller no
 * longer needs them. [evaluate] performs a bounded search from the given
 * position and updates [state] as progress is made.
 */
interface AnalysisEngine {
    val state: StateFlow<EngineState>

    suspend fun configure(config: EngineConfig)

    suspend fun newGame()

    /**
     * Search [position] for a best move. Returns the best move found, or
     * `null` when the position has no legal moves.
     */
    suspend fun bestMove(position: Position): EngineBest?

    /**
     * Search [position] and report a principal variation to [onLine], then
     * return the best move. Progress callbacks run on a background dispatcher.
     */
    suspend fun evaluate(position: Position, onLine: (EngineLine) -> Unit): EngineBest?

    suspend fun stop()
    suspend fun pause()
    suspend fun resume()
    fun dispose()
}