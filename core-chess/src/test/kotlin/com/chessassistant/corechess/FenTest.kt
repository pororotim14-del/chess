package com.chessassistant.corechess

import com.chessassistant.corechess.model.Position
import com.chessassistant.corechess.notation.FenParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class FenTest {

    @Test
    fun `start position round trips`() {
        val fen = Position.START_FEN
        val pos = FenParser.parse(fen)
        assertNotNull(pos)
        assertEquals(fen, pos!!.toFen())
    }

    @Test
    fun `parses a midgame fen`() {
        val fen = "r1bqkbnr/pppp1ppp/2n5/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 2 3"
        val pos = FenParser.parse(fen) ?: throw AssertionError("parse failed")
        assertEquals(fen, pos.toFen())
        assertEquals("White to move", com.chessassistant.corechess.model.Color.WHITE, pos.sideToMove)
        assertEquals(2, pos.halfmoveClock)
        assertEquals(3, pos.fullmoveNumber)
    }

    @Test
    fun `handles en passant square`() {
        val fen = "rnbqkbnr/ppp1p1pp/8/3pPp2/8/8/PPPP1PPP/RNBQKBNR w KQkq f6 0 3"
        val pos = FenParser.parse(fen) ?: throw AssertionError("parse failed")
        assertEquals("f6", com.chessassistant.corechess.model.Square.name(pos.epSquare!!))
        assertEquals(fen, pos.toFen())
    }

    @Test
    fun `rejects malformed fens`() {
        assertNull(FenParser.parse(""))
        assertNull(FenParser.parse("bogus"))
        assertNull(FenParser.parse("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/ w KQkq - 0 1"))
        assertNull(FenParser.parse("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR x KQkq - 0 1"))
    }

    @Test
    fun `missing fullmove field falls back to one`() {
        val pos = FenParser.parse("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0")
        assertNotNull(pos)
        assertEquals(1, pos!!.fullmoveNumber)
    }

    @Test
    fun `fen parser and serializer agree on castling only`() {
        val fen = "r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1"
        val pos = FenParser.parse(fen) ?: throw AssertionError("parse failed")
        assertEquals(fen, pos.toFen())
    }

    @Test
    fun `fullmove defaults to one`() {
        val fen = "8/8/8/8/8/8/8/8 w - -"
        val pos = FenParser.parse(fen) ?: throw AssertionError("parse failed")
        assertEquals(1, pos.fullmoveNumber)
        assertEquals(0, pos.halfmoveClock)
    }
}