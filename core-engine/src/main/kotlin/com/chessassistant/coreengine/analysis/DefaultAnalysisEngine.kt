package com.chessassistant.coreengine.analysis

import com.chessassistant.corechess.model.Color
import com.chessassistant.corechess.model.Move
import com.chessassistant.corechess.model.Position
import com.chessassistant.corechess.rules.MoveGenerator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/**
 * A lightweight pure-Kotlin analysis backend used as the on-device engine.
 * It performs a fixed-depth material search so games and analysis keep
 * working even without a bundled native engine.
 */
class DefaultAnalysisEngine : AnalysisEngine {

    private val _state = MutableStateFlow<EngineState>(EngineState.Idle)
    override val state: StateFlow<EngineState> = _state

    @Volatile
    private var running = false

    @Volatile
    private var depthLimit = DEFAULT_DEPTH

    override suspend fun configure(config: EngineConfig) {
        depthLimit = config.depth.coerceIn(1, 6)
        _state.value = EngineState.Ready
    }

    override suspend fun newGame() {
        _state.value = EngineState.Ready
    }

    override suspend fun stop() {
        running = false
        _state.value = EngineState.Stopped("stopped by user")
        _state.value = EngineState.Ready
    }

    override suspend fun pause() {
        running = false
        _state.value = EngineState.Paused
    }

    override suspend fun resume() {
        _state.value = EngineState.Ready
    }

    override suspend fun bestMove(position: Position): EngineBest? {
        _state.value = EngineState.Searching
        running = true
        return try {
            withContext(Dispatchers.Default) {
                withTimeout(SEARCH_TIMEOUT_MS) { searchBest(position) }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        } finally {
            running = false
            _state.value = EngineState.Ready
        }
    }

    override suspend fun evaluate(
        position: Position,
        onLine: (EngineLine) -> Unit,
    ): EngineBest? {
        _state.value = EngineState.Searching
        running = true
        return try {
            withContext(Dispatchers.Default) {
                withTimeout(EVALUATION_TIMEOUT_MS) {
                    val result = searchBest(position)
                    if (result != null) {
                        onLine(EngineLine(result.pv, result.score))
                    }
                    result
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        } finally {
            running = false
            _state.value = EngineState.Ready
        }
    }

    private fun searchBest(position: Position): EngineBest? {
        val legal = MoveGenerator.legalMoves(position)
        if (legal.isEmpty()) return null

        var best: Move? = null
        var bestScore = Int.MIN_VALUE

        for (move in legal) {
            val next = position.apply(move)
            val raw = negamax(next, depthLimit - 1, Int.MIN_VALUE / 2, Int.MAX_VALUE / 2)
            val score = if (next.sideToMove == Color.BLACK) raw else -raw
            if (score > bestScore) {
                bestScore = score
                best = move
            }
            if (!running) break
        }

        return best?.let {
            EngineBest(
                move = it.uci,
                pv = bestLine(position, it),
                score = EngineScore(bestScore),
            )
        }
    }

    private fun bestLine(position: Position, first: Move): List<String> {
        val pv = mutableListOf(first.uci)
        var plies = 2
        var cur: Position? = position.apply(first)
        while (plies-- > 0 && cur != null) {
            val nextMove = findBestQuiet(cur)
            if (nextMove == null) break
            pv += nextMove.uci
            cur = cur.apply(nextMove)
        }
        return pv
    }

    private fun findBestQuiet(position: Position): Move? {
        val legal = MoveGenerator.legalMoves(position)
        var best: Move? = null
        var bestScore = Int.MIN_VALUE
        for (move in legal) {
            val next = position.apply(move)
            val raw = negamax(next, 1, Int.MIN_VALUE / 2, Int.MAX_VALUE / 2)
            val score = if (next.sideToMove == Color.BLACK) raw else -raw
            if (score > bestScore) {
                bestScore = score
                best = move
            }
        }
        return best
    }

    @Suppress("ReturnCount")
    private fun negamax(position: Position, depth: Int, alphaIn: Int, betaIn: Int): Int {
        if (depth <= 0) return evaluate(position)
        val legal = MoveGenerator.legalMoves(position)
        if (legal.isEmpty()) {
            return if (position.isInCheck(position.sideToMove)) {
                Int.MIN_VALUE / 4
            } else {
                0
            }
        }
        var alpha = alphaIn
        var beta = betaIn
        var score = Int.MIN_VALUE / 2
        for (move in legal) {
            val next = position.apply(move)
            val value = -negamax(next, depth - 1, -beta, -alpha)
            if (value > score) score = value
            if (score > alpha) alpha = score
            if (alpha >= beta) break
            if (!running) break
        }
        return score
    }

    private fun evaluate(position: Position): Int {
        var score = 0
        for (p in position.board) {
            if (p == null) continue
            val v = PIECE_VALUES[p.type.ordinal]
            score += if (p.color == Color.WHITE) v else -v
        }
        return if (position.sideToMove == Color.WHITE) score else -score
    }

    override fun dispose() {
        running = false
    }

    companion object {
        private const val DEFAULT_DEPTH = 3
        private const val SEARCH_TIMEOUT_MS = 20_000L
        private const val EVALUATION_TIMEOUT_MS = 60_000L

        // PAWN, KNIGHT, BISHOP, ROOK, QUEEN, KING
        private val PIECE_VALUES = intArrayOf(100, 300, 320, 500, 900, 0)
    }
}