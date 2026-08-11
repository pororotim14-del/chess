package com.chessassistant.corechess

import com.chessassistant.corechess.model.Position
import com.chessassistant.corechess.notation.FenParser
import com.chessassistant.corechess.rules.Perft
import org.junit.Test

/** TEMPORARY diagnostic: prints per-division counts for analysis. */
class DivideDiagTest {

    private fun dump(fen: String, depth: Int, label: String) {
        val pos = FenParser.parse(fen) ?: return
        val counts = Perft.divideCounts(pos, depth)
        println("== $label divide depth=$depth ==")
        for ((m, c) in counts.entries.sortedBy { it.key }) {
            println("$m: $c")
        }
    }

    @Test
    fun dumpStartDepth3() {
        dump(Position.START_FEN, 3, "startpos")
    }

    @Test
    fun dumpPos2() {
        dump("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1", 2, "pos2")
    }

    @Test
    fun dumpPos3() {
        dump("8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1", 1, "pos3")
    }

    @Test
    fun dumpPos6() {
        dump("r4rk1/1pp1qppp/p1np1n2/2b1p1B1/2B1P1b1/P1NP1N2/1PP1QPPP/R4RK1 w - - 0 10", 2, "pos6")
    }
}