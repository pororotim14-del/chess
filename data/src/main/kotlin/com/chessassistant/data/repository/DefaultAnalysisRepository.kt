package com.chessassistant.data.repository

import com.chessassistant.corechess.model.Position
import com.chessassistant.corechess.notation.San
import com.chessassistant.coreengine.analysis.AnalysisEngine
import com.chessassistant.coreengine.analysis.EngineBest
import com.chessassistant.coreengine.analysis.EngineLine
import com.chessassistant.domain.model.PositionAnalysis
import com.chessassistant.domain.model.ScoredMove
import com.chessassistant.domain.repository.AnalysisRepository
import com.chessassistant.domain.repository.AnalysisState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope

/**
 * Bridges the engine backend to the domain [AnalysisRepository].
 */
class DefaultAnalysisRepository(
    private val engine: AnalysisEngine,
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher,
) : AnalysisRepository {

    private val _state = MutableStateFlow<AnalysisState>(AnalysisState.Idle)
    override val state: StateFlow<AnalysisState> = _state.asStateFlow()

    private val _progress = MutableSharedFlow<PositionAnalysis>(extraBufferCapacity = 1)
    override val progress: Flow<PositionAnalysis> = _progress.asSharedFlow()

    @Volatile
    private var currentJob: kotlinx.coroutines.Job? = null

    override fun analyze(position: Position) {
        stop()
        _state.value = AnalysisState.Analyzing
        currentJob = scope.launch(dispatcher) {
            val best = engine.bestMove(position)
            _state.value = if (best == null) {
                AnalysisState.Failed("no legal moves")
            } else {
                AnalysisState.Result(toAnalysis(position, 0, best, emptyList()))
            }
        }
    }

    override fun stop() {
        currentJob?.cancel()
        currentJob = null
        _state.value = AnalysisState.Idle
    }

    override fun clear() {
        stop()
        _state.value = AnalysisState.Idle
    }

    override fun evaluationRatio(analysis: PositionAnalysis): Float {
        val ev = analysis.evaluation
        return when {
            analysis.isMate -> if (ev > 0) 1f else -1f
            else -> (ev / 900f).coerceIn(-1f, 1f)
        }
    }

    private fun toAnalysis(position: Position, ply: Int, best: EngineBest, extra: List<ScoredMove>): PositionAnalysis {
        val moves = (listOf(best.move) + best.pv.take(4).drop(1)).distinct()
        val scored = moves.mapNotNull { uci ->
            com.chessassistant.corechess.model.Move.fromUci(uci)?.let {
                val san = San.format(position, it)
                ScoredMove(uci = uci, san = san, cp = best.score.centipawns, mateIn = best.score.mateIn)
            }
        }
        return PositionAnalysis(
            fen = position.toFen(),
            ply = ply,
            bestLine = if (scored.isEmpty()) extra else scored + extra,
            evaluation = best.score.centipawns.toFloat() / 100f,
        )
    }
}