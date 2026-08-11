package com.chessassistant.coreengine.analysis

import com.chessassistant.corechess.model.Position
import com.chessassistant.corechess.notation.FenParser
import com.chessassistant.corechess.rules.MoveGenerator
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DefaultAnalysisEngineTest {

    @Test
    fun `returns a legal best move for a quiet middle game`() = runTest {
        val engine = DefaultAnalysisEngine()
        val position = FenParser.parse(
            "r1bqkb1r/pppp1ppp/2n2n2/4p3/2B1P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 4 4",
        )!!

        val best = engine.bestMove(position)

        assertNotNull(best)
        val legal = MoveGenerator.legalMoves(position).map { it.uci }
        assertEquals(true, best!!.move in legal)
        assertEquals(EngineState.Ready, engine.state.value)
        engine.dispose()
    }

    @Test
    fun `returns null when the side to move has no legal moves`() = runTest {
        val engine = DefaultAnalysisEngine()
        // Scholar's mate: black is checkmated (side to move = black).
        val position = FenParser.parse(
            "rnbqkb1r/pppp1Qpp/5n2/4p3/2B5/8/PPP1PPPP/RNB1K1NR b KQkq - 1 4",
        )!!

        val best = engine.bestMove(position)

        assertNull(best)
        engine.dispose()
    }

    @Test
    fun `state transitions through searching to ready`() = runTest {
        val engine = DefaultAnalysisEngine()
        val position = Position.STARTING

        assertEquals(EngineState.Idle, engine.state.value)
        engine.bestMove(position)
        assertEquals(EngineState.Ready, engine.state.value)
        engine.dispose()
    }
}