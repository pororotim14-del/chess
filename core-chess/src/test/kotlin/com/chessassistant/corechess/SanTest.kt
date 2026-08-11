package com.chessassistant.corechess

import com.chessassistant.corechess.model.Position
import com.chessassistant.corechess.notation.FenParser
import com.chessassistant.corechess.notation.San
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SanTest {

    private fun pos(fen: String): Position =
        FenParser.parse(fen) ?: throw AssertionError("bad fen: $fen")

    @Test
    fun `basic move`() {
        val p = Position.STARTING
        val move = San.parse(p, "e4")!!
        assertEquals("e2e4", move.uci)
    }

    @Test
    fun `check and mate suffixes`() {
        val p = pos("r1bqkb1r/pppp1ppp/2n2n2/4p2Q/2B1P3/8/PPPP1PPP/RNB1K1NR w KQkq - 4 4")
        val move = San.parse(p, "Qxf7#")
        assertNotNull(move)
        assertEquals("h5f7", move!!.uci)

        val checkMove = San.parse(p, "Qh4")
        assertNotNull(checkMove)
    }

    @Test
    fun `castling notation`() {
        val p = pos("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1")
        val moveKs = San.parse(p, "O-O")!!
        assertEquals("e1g1", moveKs.uci)
        val moveQs = San.parse(p, "O-O-O")!!
        assertEquals("e1c1", moveQs.uci)
        val black = pos("r3k2r/8/8/8/8/8/8/R3K2R b KQkq - 0 1")
        assertEquals("e8g8", San.parse(black, "O-O")!!.uci)
        assertEquals("e8c8", San.parse(black, "O-O-O")!!.uci)
    }

    @Test
    fun `promotion both spellings`() {
        val p = pos("8/P6k/8/8/8/8/8/K7 w - - 0 1")
        assertEquals("a7a8q", San.parse(p, "a8=Q")!!.uci)
        assertEquals("a7a8n", San.parse(p, "a8N")!!.uci)
    }

    @Test
    fun `en passant`() {
        val p = pos("rnbqkbnr/ppp1p1pp/8/3pPp2/8/8/PPPP1PPP/RNBQKBNR w KQkq f6 0 3")
        val move = San.parse(p, "exf6")!!
        assertEquals("e5f6", move.uci)
    }

    @Test
    fun `knight no disambiguation when only one knight`() {
        val p = pos("r1bqkb1r/pppp1ppp/2n5/1B2p3/8/2N5/PPPP1PPP/R1BQK2R w KQkq - 4 4")
        assertEquals("Nd5", San.format(p, com.chessassistant.corechess.model.Move.fromUci("c3d5")!!))
        assertEquals("Na4", San.format(p, com.chessassistant.corechess.model.Move.fromUci("c3a4")!!))
    }

    @Test
    fun `knight file disambiguation when needed`() {
        // Two white knights (c3 and d2) can both reach e4 -> "Nce4" / "Nde4".
        val p = pos("r1bqkb1r/pppp1ppp/2n5/1B2p3/8/2N5/PPPNPPPP/R1BQK2R w KQkq - 4 4")
        assertEquals("Nce4", San.format(p, com.chessassistant.corechess.model.Move.fromUci("c3e4")!!))
        assertEquals("Nde4", San.format(p, com.chessassistant.corechess.model.Move.fromUci("d2e4")!!))
    }

    @Test
    fun `knight rank disambiguation when needed`() {
        // Two white knights on the same file (a2, a6) can both reach b4 -> "N2b4" / "N6b4".
        val p = pos("rnbqkbnr/pppppppp/N7/8/8/8/N7/RNBQKBNR w KQkq - 0 1")
        assertEquals("N2b4", San.format(p, com.chessassistant.corechess.model.Move.fromUci("a2b4")!!))
        assertEquals("N6b4", San.format(p, com.chessassistant.corechess.model.Move.fromUci("a6b4")!!))
    }

    @Test
    fun `format round trips with parse`() {
        val p = pos("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1")
        for (m in com.chessassistant.corechess.rules.MoveGenerator.legalMoves(p)) {
            val san = San.format(p, m)
            val back = San.parse(p, san)
            assertEquals(m, back)
        }
    }

    @Test
    fun `pawn capture file prefix`() {
        val p = pos("rnbqkbnr/ppp1pppp/8/3p4/4P3/8/PPPP1PPP/RNBQKBNR w KQkq d6 0 2")
        val move = San.parse(p, "exd5")!!
        assertEquals("e4d5", move.uci)
    }

    @Test
    fun `unknown san returns null`() {
        assertNull(San.parse(Position.STARTING, "g5"))
    }

    @Test
    fun `fools mate formatter marks mate`() {
        val p = Position.STARTING.applyUcis(listOf("f2f3", "e7e5", "g2g4"))
        val san = San.format(p, com.chessassistant.corechess.model.Move.fromUci("d8h4")!!)
        assertEquals("Qh4#", san)
    }

    @Test
    fun `mate by quiet move not marked as check`() {
        // Fool's mate: f2-f3 then g2-g4 allows Qxh4# for black.
        val p = Position.STARTING.applyUcis(listOf("f2f3", "e7e5", "g2g4"))
        val mate = San.parse(p, "Qh4#")
        assertNotNull(mate)
        assertEquals("d8h4", mate!!.uci)
    }
}