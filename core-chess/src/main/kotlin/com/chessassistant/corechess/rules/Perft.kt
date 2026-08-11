package com.chessassistant.corechess.rules

import com.chessassistant.corechess.model.Position

/** Perft (performance test) node counter used to verify move generation. */
object Perft {

    /** Number of leaf nodes at the given depth from [pos]. */
    fun count(pos: Position, depth: Int): Long {
        if (depth == 0) return 1
        var nodes = 0L
        for (m in MoveGenerator.legalMoves(pos)) {
            nodes += count(pos.apply(m), depth - 1)
        }
        return nodes
    }

    /** Per-root-move node counts at [depth] (used by "divide" style verification). */
    fun divideCounts(pos: Position, depth: Int): Map<String, Long> {
        val result = LinkedHashMap<String, Long>()
        for (m in MoveGenerator.legalMoves(pos)) {
            result[m.uci] = count(pos.apply(m), depth - 1)
        }
        return result
    }
}