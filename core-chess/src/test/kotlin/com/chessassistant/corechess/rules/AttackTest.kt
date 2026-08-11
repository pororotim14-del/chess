package com.chessassistant.corechess.rules

import com.chessassistant.corechess.model.Color
import com.chessassistant.corechess.model.Move
import com.chessassistant.corechess.notation.FenParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression coverage for attack detection on edge files (a/h) where the
 * flat 0x88-free 64-array indexing can wrap across ranks.
 */
class AttackTest {

    @Test
    fun `black pawn on g4 attacks h3`() {
        val pos = FenParser.parse("4k3/8/8/8/6p1/7K/8/4R3 w - - 0 1")!!
        val h3 = com.chessassistant.corechess.model.Square.index(7, 2)

        assertTrue(Attack.isSquareAttacked(pos.board, h3, Color.BLACK))
    }

    @Test
    fun `white pawn on b2 attacks a3 and c3`() {
        val pos = FenParser.parse("4k3/8/8/8/8/8/1P6/4K3 w - - 0 1")!!
        val a3 = com.chessassistant.corechess.model.Square.index(0, 2)
        val c3 = com.chessassistant.corechess.model.Square.index(2, 2)
        val a1 = com.chessassistant.corechess.model.Square.index(0, 0)

        assertTrue(Attack.isSquareAttacked(pos.board, a3, Color.WHITE))
        assertTrue(Attack.isSquareAttacked(pos.board, c3, Color.WHITE))
        assertFalse(Attack.isSquareAttacked(pos.board, a1, Color.WHITE))
    }

    @Test
    fun `king cannot stay in check from edge-file pawn`() {
        // White king h3 is attacked by the black g4 pawn; any quiet move
        // leaving the king on h3 must be illegal, Kxg4 must be legal.
        val pos = FenParser.parse("4k3/8/8/8/6p1/7K/8/4R3 w - - 0 1")!!
        val legal = MoveGenerator.legalMoves(pos).map { it.uci }

        assertTrue(legal.contains("h3g4"))
        assertFalse(legal.contains("a1a2"))
        assertFalse(legal.contains("e1e2"))
    }

    @Test
    fun `rook sliding off a-file edge is bounded`() {
        // White rook a1 to a8: must stop at a8 and not wrap to b1.
        val pos = FenParser.parse("8/8/8/8/8/8/8/R3K3 w - - 0 1")!!
        val legal = MoveGenerator.legalMoves(pos).map { it.uci }

        assertTrue(legal.contains("a1a8"))
        assertFalse(legal.contains("a1b2"))
        assertFalse(legal.contains("a1a9"))
    }
}