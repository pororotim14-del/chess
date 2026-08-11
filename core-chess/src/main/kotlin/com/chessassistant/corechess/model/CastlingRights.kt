package com.chessassistant.corechess.model

/** Castling right bitmask. */
object CastlingRights {
    const val WHITE_KINGSIDE = 1 shl 0
    const val WHITE_QUEENSIDE = 1 shl 1
    const val BLACK_KINGSIDE = 1 shl 2
    const val BLACK_QUEENSIDE = 1 shl 3
    const val ALL = WHITE_KINGSIDE or WHITE_QUEENSIDE or BLACK_KINGSIDE or BLACK_QUEENSIDE
    const val NONE = 0

    fun has(castling: Int, right: Int): Boolean = (castling and right) != 0

    /** FEN string like "KQkq" for [castling]; "-" when no rights remain. */
    fun toFen(castling: Int): String {
        val sb = StringBuilder()
        if (has(castling, WHITE_KINGSIDE)) sb.append('K')
        if (has(castling, WHITE_QUEENSIDE)) sb.append('Q')
        if (has(castling, BLACK_KINGSIDE)) sb.append('k')
        if (has(castling, BLACK_QUEENSIDE)) sb.append('q')
        return if (sb.isEmpty()) "-" else sb.toString()
    }

    fun fromFen(fen: String): Int {
        if (fen.isEmpty() || fen == "-") return NONE
        var rights = NONE
        for (c in fen) {
            when (c) {
                'K' -> rights = rights or WHITE_KINGSIDE
                'Q' -> rights = rights or WHITE_QUEENSIDE
                'k' -> rights = rights or BLACK_KINGSIDE
                'q' -> rights = rights or BLACK_QUEENSIDE
            }
        }
        return rights
    }
}