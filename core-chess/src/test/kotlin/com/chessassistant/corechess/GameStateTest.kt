package com.chessassistant.corechess

import com.chessassistant.corechess.model.GameState
import com.chessassistant.corechess.rules.GameStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameStateTest {

    @Test
    fun `plays moves and tracks position`() {
        val g = GameState()
        assertTrue(g.playUci("e2e4"))
        assertTrue(g.playUci("e7e5"))
        assertTrue(g.playUci("g1f3"))
        assertEquals("rnbqkbnr/pppp1ppp/8/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R b KQkq - 1 2", g.fen())
        assertEquals(3, g.playedMoves().size)
    }

    @Test
    fun `rejects illegal moves`() {
        val g2 = GameState()
        assertFalse(g2.playUci("e2e5")) // pawn cannot jump 3 squares
        assertFalse(g2.playUci("a2a4b4")) // malformed uci
        assertFalse(g2.playUci("e2e4g2")) // malformed promotion
        assertFalse(g2.playUci("e1c1")) // no castling rights yet
    }

    @Test
    fun `undo and redo round trip`() {
        val g = GameState()
        g.playUci("e2e4")
        g.playUci("e7e5")
        val fenAfter = g.fen()
        assertTrue(g.canUndo)
        g.undo()
        assertTrue(g.canRedo)
        assertEquals("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1", g.fen())
        g.redo()
        assertEquals(fenAfter, g.fen())
    }

    @Test
    fun `undo collapses redo on new move`() {
        val g = GameState()
        g.playUci("e2e4")
        g.playUci("e7e5")
        g.undo()
        g.playUci("c7c5")
        assertFalse(g.canRedo)
        assertEquals(2, g.playedMoves().size)
    }

    @Test
    fun `fools mate ends in checkmate`() {
        val g = GameState()
        g.playUci("f2f3")
        g.playUci("e7e5")
        g.playUci("g2g4")
        g.playUci("d8h4")
        assertEquals(GameStatus.CHECKMATE, g.gameStatus())
    }

    @Test
    fun `starts from custom fen`() {
        val g = GameState("r3k2r/8/8/8/8/8/8/R3K2R b KQkq - 0 1")
        assertTrue(g.playUci("e8g8"))
        assertTrue(g.playUci("a1a2"))
    }

    @Test
    fun `en passant is playable and undoable`() {
        val g = GameState("rnbqkbnr/ppp1p1pp/8/3pPp2/8/8/PPPP1PPP/RNBQKBNR w KQkq f6 0 3")
        assertTrue(g.playUci("e5f6"))
        assertEquals("rnbqkbnr/ppp1p1pp/5P2/3p4/8/8/PPPP1PPP/RNBQKBNR b KQkq - 0 3", g.fen())
        g.undo()
        assertEquals("rnbqkbnr/ppp1p1pp/8/3pPp2/8/8/PPPP1PPP/RNBQKBNR w KQkq f6 0 3", g.fen())
    }
}