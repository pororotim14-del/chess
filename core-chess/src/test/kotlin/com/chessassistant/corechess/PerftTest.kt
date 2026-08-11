package com.chessassistant.corechess

import com.chessassistant.corechess.model.Position
import com.chessassistant.corechess.notation.FenParser
import com.chessassistant.corechess.rules.Perft
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Well-known perft values:
 *  startpos: d1=20 d2=400 d3=8902 d4=197281 d5=4865609
 *  position 2: d1=48 d2=2039 d3=97862
 *  position 3 (en passant): d1=14 d2=191 d3=2812
 *  position 6: d1=46 d2=2079 d3=89890
 */
class PerftTest {

    @Test
    fun `start position depth 1-4`() {
        val pos = Position.STARTING
        assertEquals(20L, Perft.count(pos, 1))
        assertEquals(400L, Perft.count(pos, 2))
        assertEquals(8902L, Perft.count(pos, 3))
        assertEquals(197281L, Perft.count(pos, 4))
    }

    @Test
    fun `position 2 (castling and captures depth 2)`() {
        val fen = "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1"
        val pos = FenParser.parse(fen) ?: throw AssertionError("bad fen")
        assertEquals(48L, Perft.count(pos, 1))
        assertEquals(2039L, Perft.count(pos, 2))
    }

    @Test
    fun `position 3 (en passant) depth 3`() {
        val fen = "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1"
        val pos = FenParser.parse(fen) ?: throw AssertionError("bad fen")
        assertEquals(14L, Perft.count(pos, 1))
        assertEquals(191L, Perft.count(pos, 2))
        assertEquals(2812L, Perft.count(pos, 3))
    }

    @Test
    fun `position 4 (en passant promotion) depth 3`() {
        val fen = "r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1"
        val pos = FenParser.parse(fen) ?: throw AssertionError("bad fen")
        assertEquals(6L, Perft.count(pos, 1))
        assertEquals(264L, Perft.count(pos, 2))
        assertEquals(9467L, Perft.count(pos, 3))
    }

    @Test
    fun `position 5 (promotion) depth 3`() {
        val fen = "rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8"
        val pos = FenParser.parse(fen) ?: throw AssertionError("bad fen")
        assertEquals(44L, Perft.count(pos, 1))
        assertEquals(1486L, Perft.count(pos, 2))
        assertEquals(62379L, Perft.count(pos, 3))
    }

    @Test
    fun `position 6 (absent castling) depth 2`() {
        val fen = "r4rk1/1pp1qppp/p1np1n2/2b1p1B1/2B1P1b1/P1NP1N2/1PP1QPPP/R4RK1 w - - 0 10"
        val pos = FenParser.parse(fen) ?: throw AssertionError("bad fen")
        assertEquals(46L, Perft.count(pos, 1))
        assertEquals(2079L, Perft.count(pos, 2))
    }
}