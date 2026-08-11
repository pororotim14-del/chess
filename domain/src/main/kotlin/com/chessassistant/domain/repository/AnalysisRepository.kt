package com.chessassistant.domain.repository

import com.chessassistant.corechess.model.Position
import com.chessassistant.domain.model.PositionAnalysis
import com.chessassistant.domain.model.ScoredMove
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Feeds evaluation data to the UI. Implementations wrap the analysis engine.
 */
interface AnalysisRepository {

    val state: StateFlow<AnalysisState>

    /**
     * Starts (or restarts) analysis from [position]; results flow into [state].
     */
    fun analyze(position: Position)

    fun stop()

    fun clear()

    /** Callbacks fired as the search improves its principal variation. */
    val progress: Flow<PositionAnalysis>

    /** The engine's evaluation rounded into a -1..1 value for progress bars. */
    fun evaluationRatio(analysis: PositionAnalysis): Float
}

sealed interface AnalysisState {
    data object Idle : AnalysisState
    data object Analyzing : AnalysisState
    data class Result(val analysis: PositionAnalysis) : AnalysisState
    data class Failed(val reason: String) : AnalysisState
}