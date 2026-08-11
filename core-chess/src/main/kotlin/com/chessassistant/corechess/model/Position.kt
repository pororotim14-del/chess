package com.chessassistant.corechess.model

import com.chessassistant.corechess.notation.FenParser
import com.chessassistant.corechess.rules.Attack
import kotlin.math.abs

/**
 * Immutable chess position. Every move produces a new [Position].
 *
 * [board] is indexed by the flat square index (file + rank*8); null = empty.
 */
data class Position(
    val board: Array<Piece?>,
    val sideToMove: Color,
    val castlingRights: Int,
    val epSquare: Int?,
    val halfmoveClock: Int,
    val fullmoveNumber: Int,
) {
    fun pieceAt(sq: Int): Piece? = board[sq]

    fun findKing(color: Color): Int {
        for (s in 0..63) {
            val p = board[s]
            if (p != null && p.type == PieceType.KING && p.color == color) return s
        }
        throw IllegalStateException("No $color king on board")
    }

    fun kingSquare(color: Color): Int = findKing(color)

    fun isInCheck(color: Color): Boolean =
        Attack.isSquareAttacked(board, findKing(color), color.opposite)

    /** True when [color] has at least one legal move. */
    fun hasAnyLegalMove(color: Color): Boolean {
        return color == sideToMove &&
            com.chessassistant.corechess.rules.MoveGenerator.legalMoves(this).isNotEmpty()
    }

    /**
     * Applies [move], returning the resulting position.
     * Assumes the move is pseudo-legal for this position.
     */
    fun apply(move: Move): Position {
        val nb = board.copyOf()
        val piece = nb[move.from] ?: throw IllegalArgumentException(
            "No piece on ${Square.name(move.from)}",
        )
        val color = piece.color

        nb[move.from] = null

        var isEnPassant = false
        var capturedSq: Int? = null
        if (nb[move.to] != null) {
            capturedSq = move.to
        } else if (piece.type == PieceType.PAWN && move.to == epSquare) {
            isEnPassant = true
            capturedSq = if (color == Color.WHITE) move.to - 8 else move.to + 8
            nb[capturedSq!!] = null
        }

        var castling = castlingRights
        when (move.to) {
            Square.A1 -> castling = castling and CastlingRights.WHITE_QUEENSIDE.inv()
            Square.H1 -> castling = castling and CastlingRights.WHITE_KINGSIDE.inv()
            Square.A8 -> castling = castling and CastlingRights.BLACK_QUEENSIDE.inv()
            Square.H8 -> castling = castling and CastlingRights.BLACK_KINGSIDE.inv()
        }

        if (piece.type == PieceType.KING) {
            if (abs(com.chessassistant.corechess.model.Square.file(move.to) -
                    com.chessassistant.corechess.model.Square.file(move.from)) == 2
            ) {
                val kingRank = com.chessassistant.corechess.model.Square.rank(move.from)
                val kingside = com.chessassistant.corechess.model.Square.file(move.to) == 6
                val rookFrom = Square.index(if (kingside) 7 else 0, kingRank)
                val rookTo = Square.index(if (kingside) 5 else 3, kingRank)
                nb[rookTo] = nb[rookFrom]
                nb[rookFrom] = null
            }
            val rightsOfColor =
                if (color == Color.WHITE) CastlingRights.WHITE_KINGSIDE or CastlingRights.WHITE_QUEENSIDE
                else CastlingRights.BLACK_KINGSIDE or CastlingRights.BLACK_QUEENSIDE
            castling = castling and rightsOfColor.inv()
        } else if (piece.type == PieceType.ROOK) {
            val cleared = when (move.from) {
                Square.A1 -> CastlingRights.WHITE_QUEENSIDE
                Square.H1 -> CastlingRights.WHITE_KINGSIDE
                Square.A8 -> CastlingRights.BLACK_QUEENSIDE
                Square.H8 -> CastlingRights.BLACK_KINGSIDE
                else -> CastlingRights.NONE
            }
            castling = castling and cleared.inv()
        }

        nb[move.to] = if (move.promotion != null) Piece(move.promotion, color) else piece

        val newEp: Int? =
            if (piece.type == PieceType.PAWN &&
                abs(Square.rank(move.to) - Square.rank(move.from)) == 2
            ) {
                if (color == Color.WHITE) move.from + 8 else move.from - 8
            } else {
                null
            }

        val halfmove = if (piece.type == PieceType.PAWN || capturedSq != null) 0 else halfmoveClock + 1
        val fullmove = if (color == Color.BLACK) fullmoveNumber + 1 else fullmoveNumber

        return Position(nb, color.opposite, castling, newEp, halfmove, fullmove)
    }

    /** Applies a sequence of moves in order. */
    fun apply(moves: List<Move>): Position {
        var pos = this
        for (m in moves) pos = pos.apply(m)
        return pos
    }

    /** Applies a single UCI move string; returns null when malformed/illegal. */
    fun applyUci(uci: String): Position? {
        val move = Move.fromUci(uci) ?: return null
        if (com.chessassistant.corechess.rules.MoveGenerator.legalMoves(this).none { it == move }) {
            return null
        }
        return apply(move)
    }

    /** Applies a list of UCI move strings, stopping at the first failure. */
    fun applyUcis(ucis: List<String>): Position {
        var pos = this
        for (u in ucis) {
            val move = Move.fromUci(u) ?: break
            pos = pos.apply(move)
        }
        return pos
    }

    fun toFen(): String = com.chessassistant.corechess.notation.FenSerializer.serialize(this)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Position) return false
        if (!board.contentEquals(other.board)) return false
        if (sideToMove != other.sideToMove) return false
        if (castlingRights != other.castlingRights) return false
        if (epSquare != other.epSquare) return false
        if (halfmoveClock != other.halfmoveClock) return false
        if (fullmoveNumber != other.fullmoveNumber) return false
        return true
    }

    override fun hashCode(): Int {
        var result = board.contentHashCode()
        result = 31 * result + sideToMove.hashCode()
        result = 31 * result + castlingRights
        result = 31 * result + (epSquare ?: -1)
        result = 31 * result + halfmoveClock
        result = 31 * result + fullmoveNumber
        return result
    }

    companion object {
        const val START_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
        const val START_FEN_SHORT = "8/8/8/8/8/8/8/8 w - - 0 1"

        val STARTING: Position = FenParser.parseOrThrow(START_FEN)
    }
}