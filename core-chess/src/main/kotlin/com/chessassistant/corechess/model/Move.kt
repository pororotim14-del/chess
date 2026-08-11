package com.chessassistant.corechess.model

/**
 * A chess move between two squares, optionally promoting.
 *
 * Special moves (castling, en passant, double pawn push) are fully derivable
 * from the position plus from/to, so no extra flags are stored here.
 */
data class Move(
    val from: Int,
    val to: Int,
    val promotion: PieceType? = null,
) {
    /** UCI notation, e.g. "e2e4", "e7e8q", castle as "e1g1". */
    val uci: String
        get() = Square.name(from) + Square.name(to) + (promotion?.fenChar ?: "")

    companion object {
        fun fromUci(uci: String): Move? {
            if (uci.length !in 4..5) return null
            val from = Square.fromName(uci.substring(0, 2)) ?: return null
            val to = Square.fromName(uci.substring(2, 4)) ?: return null
            var promotion: PieceType? = null
            if (uci.length == 5) {
                promotion = PieceType.fromFen(uci[4]) ?: return null
                if (promotion == PieceType.KING || promotion == PieceType.PAWN) return null
            }
            return Move(from, to, promotion)
        }
    }
}