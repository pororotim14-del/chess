package com.chessassistant.corechess.notation

import com.chessassistant.corechess.model.CastlingRights
import com.chessassistant.corechess.model.Color
import com.chessassistant.corechess.model.Piece
import com.chessassistant.corechess.model.Position
import com.chessassistant.corechess.model.Square

/** Parses Forsyth-Edwards notation into a [Position]. */
object FenParser {

    /** Parses a FEN string; returns null when the input is structurally invalid. */
    fun parse(fen: String): Position? {
        val parts = fen.trim().split(Regex("\\s+"))
        if (parts.isEmpty() || parts.size < 4) return null

        val board = arrayOfNulls<Piece?>(64)
        val rows = parts[0].split('/')
        if (rows.size != 8) return null
        var rank = 7
        for (rowText in rows) {
            var file = 0
            for (c in rowText) {
                if (c in '1'..'8') {
                    file += c - '0'
                    if (file > 8) return null
                } else {
                    if (file !in 0..7) return null
                    val piece = Piece.fromFen(c) ?: return null
                    board[Square.index(file, rank)] = piece
                    file++
                    if (file > 8) return null
                }
            }
            if (file != 8) return null
            rank--
        }
        if (rank != -1) return null

        val side = when (parts[1]) {
            "w" -> Color.WHITE
            "b" -> Color.BLACK
            else -> return null
        }

        val castling = CastlingRights.fromFen(parts[2])

        val epText = parts[3]
        val ep = if (epText == "-") null else Square.fromName(epText)
        if (epText != "-" && ep == null) return null

        val halfmove = parts.getOrNull(4)?.toIntOrNull() ?: 0
        val fullmove = parts.getOrNull(5)?.toIntOrNull() ?: 1

        return Position(
            board = board,
            sideToMove = side,
            castlingRights = castling,
            epSquare = ep,
            halfmoveClock = halfmove.coerceAtLeast(0),
            fullmoveNumber = fullmove.coerceAtLeast(1),
        )
    }

    fun parseOrThrow(fen: String): Position =
        parse(fen) ?: throw IllegalArgumentException("Invalid FEN: \"$fen\"")
}