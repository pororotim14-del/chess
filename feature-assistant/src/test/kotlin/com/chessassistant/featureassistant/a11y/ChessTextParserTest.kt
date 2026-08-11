package com.chessassistant.featureassistant.a11y

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChessTextParserTest {

    @Test
    fun `detects a literal FEN`() {
        val fen = "r1bqkbnr/pppp1ppp/2n5/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 2 3"
        val result = ChessTextParser.parse(listOf("Engine vs Engine", fen, "0:00"))
        assertTrue(result.detected)
        assertEquals(fen, result.fen)
    }

    @Test
    fun `replays SAN move list from the start position`() {
        val result = ChessTextParser.parse(
            listOf("1. e4 e5 2. Nf3 Nc6 3. Bb5 a6"),
        )
        assertTrue(result.detected)
        val fen = result.fen!!
        assertTrue(fen.startsWith("r1bqkbnr/1ppp1ppp/p1n5/1B2p3/4P3/5N2/PPPP1PPP/RNBQK2R"))
        assertEquals(6, result.moves.size)
    }

    @Test
    fun `replays SAN with castling`() {
        val result = ChessTextParser.parse(
            listOf("1. e4 e5 2. Nf3 Nc6 3. Bc4 Nf6 4. 0-0"),
        )
        assertTrue(result.detected)
        // White king ends on g1 after O-O.
        assertTrue(result.fen!!.contains("RNBQ1RK1"))
    }

    @Test
    fun `falls back to UCI strings`() {
        val result = ChessTextParser.parse(listOf("e2e4 e7e5 g1f3"))
        assertTrue(result.detected)
        assertEquals(3, result.moves.size)
        assertTrue(result.fen!!.contains("RNBQKB1R"))
    }

    @Test
    fun `ignores non-chess text`() {
        val result = ChessTextParser.parse(listOf("Good evening", "Rating 2000", "1 hour"))
        assertNull(result.fen)
    }

    @Test
    fun `illegal move suffix cannot be replayed from start`() {
        val result = ChessTextParser.parse(listOf("Ke4 Qh5"))
        assertNull(result.fen)
    }

    @Test
    fun `tokenizes move numbers attached to moves`() {
        val tokens = ChessTextParser.tokenizeSans("1.e4 1...e5 2.Nf3 2...Nc6")
        assertEquals(listOf("e4", "e5", "Nf3", "Nc6"), tokens)
    }
}
