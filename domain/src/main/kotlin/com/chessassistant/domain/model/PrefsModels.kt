package com.chessassistant.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Book entry shown to the user before/after games. */
@Serializable
data class OpeningInfo(
    val name: String,
    val eco: String,
    val moves: List<String> = emptyList(),
) {
    val label: String get() = if (eco.isBlank()) name else "$eco $name"
}

/** Engine preferences the user can tune. */
@Serializable
data class EnginePrefs(
    val depth: Int = 18,
    val threads: Int = 1,
    val hashMb: Int = 32,
    val showEvaluationBar: Boolean = true,
)

/** Board preferences. */
@Serializable
data class BoardPrefs(
    val flipped: Boolean = false,
    val coordinatesVisible: Boolean = true,
    val highlightLastMove: Boolean = true,
)

/** Analysis preferences. */
@Serializable
data class AnalysisPrefs(
    val autoAnalyze: Boolean = true,
    val showBestMoveHint: Boolean = false,
)

/** Everything persisted as a single install-wide preferences blob. */
@Serializable
data class AppPrefs(
    val engine: EnginePrefs = EnginePrefs(),
    val board: BoardPrefs = BoardPrefs(),
    val analysis: AnalysisPrefs = AnalysisPrefs(),
)