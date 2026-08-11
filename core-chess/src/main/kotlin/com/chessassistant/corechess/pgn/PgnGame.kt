package com.chessassistant.corechess.pgn

import com.chessassistant.corechess.model.Color
import com.chessassistant.corechess.model.Move
import com.chessassistant.corechess.model.Position
import com.chessassistant.corechess.model.Square

/** One resolved move in a parsed game. */
data class PgnMoveRecord(
    /** SAN text exactly as it appeared (annotations removed). */
    val san: String,
    /** Internal move, null when SAN could not be resolved. */
    val move: Move?,
    /** Color that made this move. */
    val color: Color,
    /** Ply index (0-based). */
    val plyIndex: Int,
    /** Move number as shown in gamescore (1-based). */
    val moveNumber: Int,
    /** FEN right after the move, or null when unresolved. */
    val fenAfter: String?,
) {
    /** UCI coordinate notation, e.g. "e2e4". */
    val uci: String? get() = move?.uci
}

/** A parsed, mainline-only PGN game. */
data class PgnGame(
    val headers: Map<String, String>,
    val moves: List<PgnMoveRecord>,
    /** Result tag or terminal token ("1-0", "0-1", "1/2-1/2", "*"). */
    val result: String?,
    val startFen: String,
) {
    val event: String get() = headers["Event"] ?: "Untitled"
    val site: String get() = headers["Site"] ?: ""
    val date: String get() = headers["Date"] ?: ""
    val white: String get() = headers["White"] ?: "White"
    val black: String get() = headers["Black"] ?: "Black"
    val eco: String get() = headers["ECO"] ?: ""

    /** Position at the start of the game (FEN-aware). */
    fun startPosition(): Position {
        val pos = com.chessassistant.corechess.notation.FenParser.parse(startFen)
        return pos ?: Position.STARTING
    }

    /** List of SAN strings for the mainline (for display/export). */
    val sanMoves: List<String> get() = moves.map { it.san }
}

/** Result constants as they appear in PGN files. */
object PgnResult {
    const val WHITE_WINS = "1-0"
    const val BLACK_WINS = "0-1"
    const val DRAW = "1/2-1/2"
    const val OPEN = "*"
}