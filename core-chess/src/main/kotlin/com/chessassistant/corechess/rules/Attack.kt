package com.chessassistant.corechess.rules

import com.chessassistant.corechess.model.Color
import com.chessassistant.corechess.model.Piece
import com.chessassistant.corechess.model.PieceType
import com.chessassistant.corechess.model.Square
import kotlin.math.abs

/**
 * Pure attack/visibility queries on a board array.
 * No magics are used; plain stepping and ray walking is fast enough for UI
 * move generation and exact for correctness.
 */
object Attack {

    private val KNIGHT_DELTAS = intArrayOf(-17, -15, -10, -6, 6, 10, 15, 17)
    private val KING_DELTAS = intArrayOf(-9, -8, -7, -1, 1, 7, 8, 9)
    // (delta, isDiagonal, isOrthogonal)

    private fun onBoard(sq: Int): Boolean = sq in 0..63

    /** True when [s]+[d] stays on the same straight/diagonal line (no edge wrap). */
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

    /** True when [sq] is attacked by any piece of [byColor]. */
    fun isSquareAttacked(board: Array<Piece?>, sq: Int, byColor: Color): Boolean {
        // Knights and king nearby.
        for (d in KNIGHT_DELTAS) {
            val s = sq + d
            if (onBoard(s) && sameFileAware(s, sq, d)) {
                val p = board[s]
                if (p != null && p.type == PieceType.KNIGHT && p.color == byColor) return true
            }
        }
        for (d in KING_DELTAS) {
            val s = sq + d
            if (onBoard(s) && sameFileAware(s, sq, d)) {
                val p = board[s]
                if (p != null && p.type == PieceType.KING && p.color == byColor) return true
            }
        }

        val f = Square.file(sq)
        // Pawns. A white pawn attacks (file+1, rank+1) and (file-1, rank+1),
        // so the attacker for target sq sits at sq-7 (file+1, rank-1) and
        // sq-9 (file-1, rank-1). Only the corner-way square indexes wrap.
        if (byColor == Color.WHITE) {
            if (f < 7 && onBoard(sq - 7)) {
                val p = board[sq - 7]
                if (p != null && p.type == PieceType.PAWN && p.color == Color.WHITE) return true
            }
            if (f > 0 && onBoard(sq - 9)) {
                val p = board[sq - 9]
                if (p != null && p.type == PieceType.PAWN && p.color == Color.WHITE) return true
            }
        } else {
            // Black pawn attacker sits at sq+7 (file-1, rank+1) and sq+9 (file+1, rank+1).
            if (f > 0 && onBoard(sq + 7)) {
                val p = board[sq + 7]
                if (p != null && p.type == PieceType.PAWN && p.color == Color.BLACK) return true
            }
            if (f < 7 && onBoard(sq + 9)) {
                val p = board[sq + 9]
                if (p != null && p.type == PieceType.PAWN && p.color == Color.BLACK) return true
            }
        }

        // Sliders: walk each ray from sq until a blocker.
        // Orthogonal deltas: 1,-1 (horizontal) and 8,-8 (vertical).
        // Diagonal deltas: 7,-7,9,-9.
        val staticDirs = intArrayOf(1, -1, 8, -8, 7, -7, 9, -9)
        for (k in 0 until 8) {
            val delta = staticDirs[k]
            val othogonal = delta == 1 || delta == -1 || delta == 8 || delta == -8
            var s = sq + delta
            while (onRay(s - delta, delta)) {
                val p = board[s]
                if (p != null) {
                    val sliding = when (p.type) {
                        PieceType.BISHOP -> !othogonal
                        PieceType.ROOK -> othogonal
                        PieceType.QUEEN -> true
                        else -> false
                    }
                    if (p.color == byColor && sliding) return true
                    break
                }
                s += delta
            }
        }

        return false
    }

    /** Ensures the "wrap" artifact of flat 0x88 offsets is rejected. */
    private fun sameFileAware(from: Int, target: Int, delta: Int): Boolean {
        // A delta crossing the board edge changes the file by 0 unexpectedly.
        return abs(Square.file(from) - Square.file(target)) <= 2
    }
}