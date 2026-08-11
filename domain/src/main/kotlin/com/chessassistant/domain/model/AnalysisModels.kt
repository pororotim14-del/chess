package com.chessassistant.domain.model

import kotlinx.serialization.Serializable

/** A scored move produced by the analysis engine for a position. */
@Serializable
data class ScoredMove(
    val uci: String,
    val san: String,
    val cp: Int,
    val mateIn: Int? = null,
) {
    val display: String
        get() = if (mateIn != null) "M$mateIn" else "${cp / 100.0}".let { if (cp >= 0) "+$it" else it }
}

/** Full analysis snapshot for one board position. */
@Serializable
data class PositionAnalysis(
    val fen: String,
    val ply: Int,
    val bestLine: List<ScoredMove>,
    val evaluation: Float,
) {
    val isMate: Boolean get() = bestLine.any { it.mateIn != null }
}