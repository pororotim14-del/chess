package com.chessassistant.corechess.model

/** A single piece: its type and color. */
data class Piece(val type: PieceType, val color: Color) {
    /** Uppercase letter for white, lowercase for black (FEN convention). */
    val fenChar: Char
        get() = if (color == Color.WHITE) type.fenChar.uppercaseChar() else type.fenChar

    companion object {
        fun fromFen(c: Char): Piece? {
            val type = PieceType.fromFen(c) ?: return null
            val color = if (c.isUpperCase()) Color.WHITE else Color.BLACK
            return Piece(type, color)
        }
    }
}