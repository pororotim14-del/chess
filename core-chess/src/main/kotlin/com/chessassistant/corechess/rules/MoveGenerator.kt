package com.chessassistant.corechess.rules

import com.chessassistant.corechess.model.CastlingRights
import com.chessassistant.corechess.model.Color
import com.chessassistant.corechess.model.Move
import com.chessassistant.corechess.model.Piece
import com.chessassistant.corechess.model.PieceType
import com.chessassistant.corechess.model.Position
import com.chessassistant.corechess.model.Square

/** Outcome of a position extra check helper. */
enum class GameStatus { NORMAL, CHECKMATE, STALEMATE }

/**
 * Pseudo-legal and fully legal move generation for [Position].
 */
object MoveGenerator {

    private val KNIGHT_DELTAS = intArrayOf(-17, -15, -10, -6, 6, 10, 15, 17)
    private val KING_DELTAS = intArrayOf(-9, -8, -7, -1, 1, 7, 8, 9)
    private val BISHOP_DIRS = intArrayOf(7, -7, 9, -9)
    private val ROOK_DIRS = intArrayOf(1, -1, 8, -8)

    private fun onBoard(sq: Int): Boolean = sq in 0..63

    /** File-consistent jump test (rejects flat-index wraps). */
    private fun okJump(from: Int, to: Int): Boolean {
        if (!onBoard(to)) return false
        return kotlin.math.abs(Square.file(from) - Square.file(to)) <= 2
    }

    /** True when stepping [s]+[d] keeps the ray on the same diagonal/orthogonal line. */
    private fun onRay(s: Int, d: Int): Boolean {
        val t = s + d
        if (!onBoard(t)) return false
        val dirFile = when (d) {
            8, -8 -> 0
            1, 9, -7 -> 1
            else -> -1
        }
        return Square.file(t) == Square.file(s) + dirFile
    }

    private fun stepOnRay(moves: MutableList<Move>, board: Array<Piece?>, s: Int, d: Int, enemy: Color) {
        var u = s + d
        while (onRay(u - d, d)) {
            val q = board[u]
            if (q != null) {
                if (q.color == enemy && q.type != PieceType.KING) moves.add(Move(s, u))
                return
            }
            moves.add(Move(s, u))
            u += d
        }
    }

    /** All moves legal in [pos] for the side to move. */
    fun legalMoves(pos: Position): List<Move> {
        return pseudoMoves(pos).filter { applyLegal(pos, it) }
    }

    private fun applyLegal(pos: Position, m: Move): Boolean {
        val next = try {
            pos.apply(m)
        } catch (_: IllegalArgumentException) {
            return false
        }
        // The mover must not leave their own king in check.
        return !next.isInCheck(pos.sideToMove)
    }

    /** Pseudo-legal moves: castling is already restricted by the checks below. */
    fun pseudoMoves(pos: Position): List<Move> {
        val board = pos.board
        val color = pos.sideToMove
        val enemy = color.opposite
        val ep = pos.epSquare
        val moves = ArrayList<Move>(24)

        for (s in 0..63) {
            val p = board[s] ?: continue
            if (p.color != color) continue
            when (p.type) {
                PieceType.PAWN -> pawnMoves(moves, pos, s, color, ep)
                PieceType.KNIGHT -> {
                    for (d in KNIGHT_DELTAS) {
                        val t = s + d
                        if (okJump(s, t)) {
                            val q = board[t]
                            if (q == null || (q.color == enemy && q.type != PieceType.KING)) {
                                moves.add(Move(s, t))
                            }
                        }
                    }
                }
                PieceType.KING -> {
                    for (d in KING_DELTAS) {
                        val t = s + d
                        if (okJump(s, t)) {
                            val q = board[t]
                            if (q == null || q.color == enemy) moves.add(Move(s, t))
                        }
                    }
                    addCastling(moves, pos, color, s)
                }
                PieceType.BISHOP, PieceType.ROOK, PieceType.QUEEN -> {
                    val dirs = when (p.type) {
                        PieceType.BISHOP -> BISHOP_DIRS
                        PieceType.ROOK -> ROOK_DIRS
                        else -> ROOK_DIRS + BISHOP_DIRS
                    }
                    for (d in dirs) {
                        stepOnRay(moves, board, s, d, enemy)
                    }
                }
            }
        }
        return moves
    }

    private fun pawnMoves(moves: MutableList<Move>, pos: Position, s: Int, color: Color, ep: Int?) {
        val board = pos.board
        val enemy = color.opposite
        val forward = if (color == Color.WHITE) 8 else -8
        val startRank = if (color == Color.WHITE) 1 else 6
        val lastRank = if (color == Color.WHITE) 7 else 0
        val f = Square.file(s)

        // Single push.
        val one = s + forward
        if (onBoard(one) && board[one] == null) {
            if (Square.rank(one) == lastRank) {
                addPromotions(moves, s, one)
            } else {
                moves.add(Move(s, one))
            }
            // Double push.
            if (Square.rank(s) == startRank) {
                val two = s + forward * 2
                if (board[two] == null) moves.add(Move(s, two))
            }
        }

        // Captures (incl. en passant).
        fun captureTarget(targetFileOffset: Int): Int {
            val nf = f + targetFileOffset
            if (nf !in 0..7) return -1
            val t = Square.index(nf, Square.rank(one))
            if (!onBoard(t)) return -1
            return t
        }

        val left = captureTarget(-1)
        if (left != -1) {
            val q = board[left]
            if (q != null && q.color == enemy && q.type != PieceType.KING) {
                if (Square.rank(left) == lastRank) addPromotions(moves, s, left)
                else moves.add(Move(s, left))
            } else if (left == ep) {
                moves.add(Move(s, left))
            }
        }
        val right = captureTarget(1)
        if (right != -1) {
            val q = board[right]
            if (q != null && q.color == enemy && q.type != PieceType.KING) {
                if (Square.rank(right) == lastRank) addPromotions(moves, s, right)
                else moves.add(Move(s, right))
            } else if (right == ep) {
                moves.add(Move(s, right))
            }
        }
    }

    private fun addPromotions(moves: MutableList<Move>, from: Int, to: Int) {
        for (t in PROMOTION_TYPES) moves.add(Move(from, to, t))
    }

    private val PROMOTION_TYPES = arrayOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT)

    private fun addCastling(moves: MutableList<Move>, pos: Position, color: Color, kingSq: Int) {
        val board = pos.board
        val enemy = color.opposite
        val rankIdx = Square.rank(kingSq)
        val kingside = Square.index(6, rankIdx)
        val queenside = Square.index(2, rankIdx)
        if (pos.isInCheck(color)) return

        if (color == Color.WHITE && CastlingRights.has(pos.castlingRights, CastlingRights.WHITE_KINGSIDE)) {
            if (board[Square.index(5, 0)] == null && board[Square.index(6, 0)] == null) {
                if (!Attack.isSquareAttacked(board, Square.index(5, 0), enemy) &&
                    !Attack.isSquareAttacked(board, kingside, enemy)
                ) {
                    moves.add(Move(kingSq, kingside))
                }
            }
        }
        if (color == Color.WHITE && CastlingRights.has(pos.castlingRights, CastlingRights.WHITE_QUEENSIDE)) {
            if (board[Square.index(1, 0)] == null && board[Square.index(2, 0)] == null &&
                board[Square.index(3, 0)] == null
            ) {
                if (!Attack.isSquareAttacked(board, Square.index(3, 0), enemy) &&
                    !Attack.isSquareAttacked(board, queenside, enemy)
                ) {
                    moves.add(Move(kingSq, queenside))
                }
            }
        }
        if (color == Color.BLACK && CastlingRights.has(pos.castlingRights, CastlingRights.BLACK_KINGSIDE)) {
            if (board[Square.index(5, 7)] == null && board[Square.index(6, 7)] == null) {
                if (!Attack.isSquareAttacked(board, Square.index(5, 7), enemy) &&
                    !Attack.isSquareAttacked(board, kingside, enemy)
                ) {
                    moves.add(Move(kingSq, kingside))
                }
            }
        }
        if (color == Color.BLACK && CastlingRights.has(pos.castlingRights, CastlingRights.BLACK_QUEENSIDE)) {
            if (board[Square.index(1, 7)] == null && board[Square.index(2, 7)] == null &&
                board[Square.index(3, 7)] == null
            ) {
                if (!Attack.isSquareAttacked(board, Square.index(3, 7), enemy) &&
                    !Attack.isSquareAttacked(board, queenside, enemy)
                ) {
                    moves.add(Move(kingSq, queenside))
                }
            }
        }
    }

    fun gameStatus(pos: Position): GameStatus {
        if (legalMoves(pos).isNotEmpty()) return GameStatus.NORMAL
        return if (pos.isInCheck(pos.sideToMove)) GameStatus.CHECKMATE else GameStatus.STALEMATE
    }
}