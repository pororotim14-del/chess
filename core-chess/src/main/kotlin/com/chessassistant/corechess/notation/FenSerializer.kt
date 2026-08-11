package com.chessassistant.corechess.notation

import com.chessassistant.corechess.model.CastlingRights
import com.chessassistant.corechess.model.Piece
import com.chessassistant.corechess.model.Position
import com.chessassistant.corechess.model.Square

/** Serializes a [Position] into FEN notation. */
object FenSerializer {

    fun serialize(pos: Position): String {
        val sb = StringBuilder()
        for (rank in 7 downTo 0) {
            var empty = 0
            for (file in 0..7) {
                val p = pos.board[Square.index(file, rank)]
                if (p == null) {
                    empty++
                } else {
                    if (empty > 0) {
                        sb.append(empty)
                        empty = 0
                    }
                    sb.append(p.fenChar)
                }
            }
            if (empty > 0) sb.append(empty)
            if (rank > 0) sb.append('/')
        }

        sb.append(' ').append(pos.sideToMove.fenSymbol)
        sb.append(' ').append(CastlingRights.toFen(pos.castlingRights))
        sb.append(' ').append(pos.epSquare?.let { Square.name(it) } ?: "-")
        sb.append(' ').append(pos.halfmoveClock)
        sb.append(' ').append(pos.fullmoveNumber)
        return sb.toString()
    }
}