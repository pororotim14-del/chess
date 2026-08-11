package com.chessassistant.featureboard

import com.chessassistant.corechess.model.Color
import com.chessassistant.corechess.model.Position
import com.chessassistant.corechess.rules.GameStatus

data class BoardUiState(
    val fen: String = Position.START_FEN,
    val pieceSnapshot: List<com.chessassistant.corechess.model.Piece?> = emptyList(),
    val sideToMove: Color = Color.WHITE,
    val selected: Int? = null,
    val legalTargets: Set<Int> = emptySet(),
    val lastMove: Pair<Int, Int>? = null,
    val kingInCheck: Int? = null,
    val status: GameStatus = GameStatus.NORMAL,
    val outcome: String? = null,
    val flipped: Boolean = false,
    val bestMoveHint: String? = null,
    val openingName: String? = null,
    val analysisReady: Boolean = false,
) {
    companion object {
        val IDLE = BoardUiState()
    }
}