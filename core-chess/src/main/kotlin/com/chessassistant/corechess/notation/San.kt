package com.chessassistant.corechess.notation

import com.chessassistant.corechess.model.Move
import com.chessassistant.corechess.model.PieceType
import com.chessassistant.corechess.model.Position
import com.chessassistant.corechess.model.Square
import com.chessassistant.corechess.rules.GameStatus
import com.chessassistant.corechess.rules.MoveGenerator
import kotlin.math.abs

/** Formats internal moves as Standard Algebraic Notation and parses SAN back. */
object San {

    private fun pieceLetter(type: PieceType): String =
        if (type == PieceType.PAWN) "" else type.fenChar.uppercaseChar().toString()

    private fun isCastle(pos: Position, move: Move): Boolean {
        val p = pos.board[move.from] ?: return false
        return p.type == PieceType.KING && abs(Square.file(move.to) - Square.file(move.from)) == 2
    }

    /** SAN for [move] in [pos]. */
    fun format(pos: Position, move: Move): String {
        val piece = pos.board[move.from] ?: return move.uci
        if (isCastle(pos, move)) {
            return if (move.to > move.from) "O-O" else "O-O-O"
        }

        val sb = StringBuilder()
        sb.append(pieceLetter(piece.type))

        val capture = pos.board[move.to] != null ||
            (piece.type == PieceType.PAWN && move.to == pos.epSquare)
        if (piece.type == PieceType.PAWN && capture) {
            sb.append(Square.file(move.from).let { ('a'.code + it).toChar() })
        } else {
            sb.append(disambiguation(pos, move))
        }
        if (capture) sb.append('x')
        sb.append(Square.name(move.to))
        if (move.promotion != null) {
            sb.append('=').append(move.promotion.fenChar.uppercaseChar())
        }

        val next = pos.apply(move)
        if (MoveGenerator.gameStatus(next) == GameStatus.CHECKMATE) {
            sb.append('#')
        } else if (next.isInCheck(next.sideToMove)) {
            sb.append('+')
        }
        return sb.toString()
    }

    private fun disambiguation(pos: Position, move: Move): String {
        val piece = pos.board[move.from] ?: return ""
        if (piece.type == PieceType.PAWN) return ""
        val others = MoveGenerator.legalMoves(pos).filter {
            it.to == move.to && it.from != move.from && pos.board[it.from] == piece
        }
        if (others.isEmpty()) return ""
        if (others.none { Square.file(it.from) == Square.file(move.from) }) {
            return ('a'.code + Square.file(move.from)).toChar().toString()
        }
        if (others.none { Square.rank(it.from) == Square.rank(move.from) }) {
            return (Square.rank(move.from) + 1).toString()
        }
        return ('a'.code + Square.file(move.from)).toChar().toString() +
            (Square.rank(move.from) + 1).toString()
    }

    /** Normalizes common annotation trivia so comparisons are lenient. */
    fun normalize(raw: String): String = raw.trim()
        .replace("+", "")
        .replace("#", "")
        .replace("=", "")
        .replace(".e.p.", "")
        .replace(".e.p", "")
        .trimEnd('!', '?')
        .trim()

    /** Parses [sanText] (SAN, possibly with annotations) for [pos]. */
    fun parse(pos: Position, sanText: String): Move? {
        val san = normalize(sanText)
        if (san.isEmpty()) return null
        if (san == "O-O" || san == "o-o" || san == "0-0") {
            return parseCastle(pos, kingside = true)
        }
        if (san == "O-O-O" || san == "o-o-o" || san == "0-0-0") {
            return parseCastle(pos, kingside = false)
        }

        val moves = MoveGenerator.legalMoves(pos)
        for (m in moves) {
            val candidate = normalize(format(pos, m))
            if (candidate == san) return m
        }
        return null
    }

    private fun parseCastle(pos: Position, kingside: Boolean): Move? {
        val moves = MoveGenerator.legalMoves(pos)
        return moves.firstOrNull { m ->
            val p = pos.board[m.from]
            p != null && p.type == PieceType.KING &&
                (if (kingside) m.to > m.from else m.to < m.from) &&
                abs(Square.file(m.to) - Square.file(m.from)) == 2
        }
    }
}