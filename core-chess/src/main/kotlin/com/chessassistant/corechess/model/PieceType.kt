package com.chessassistant.corechess.model

/** Piece types. PAWN..KING order follows the well-known value table. */
enum class PieceType(val fenChar: Char) {
    PAWN('p'), KNIGHT('n'), BISHOP('b'), ROOK('r'), QUEEN('q'), KING('k');

    /** Piece value in centipawns used by the identity/material helpers. */
    val value: Int
        get() = when (this) {
            PAWN -> 100
            KNIGHT -> 320
            BISHOP -> 330
            ROOK -> 500
            QUEEN -> 900
            KING -> 0
        }

    /** True when the type can slide along rays (bishop/rook/queen). */
    val slider: Boolean
        get() = this == BISHOP || this == ROOK || this == QUEEN

    companion object {
        fun fromFen(c: Char): PieceType? = when (c.lowercaseChar()) {
            'p' -> PAWN
            'n' -> KNIGHT
            'b' -> BISHOP
            'r' -> ROOK
            'q' -> QUEEN
            'k' -> KING
            else -> null
        }
    }
}