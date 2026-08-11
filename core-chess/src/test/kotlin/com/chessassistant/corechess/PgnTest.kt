package com.chessassistant.corechess

import com.chessassistant.corechess.pgn.PgnParser
import com.chessassistant.corechess.pgn.PgnWriter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PgnTest {

    private val sample = """
        [Event "Test Game"]
        [Site "Localhost"]
        [Date "2024.01.01"]
        [Round "1"]
        [White "Alice"]
        [Black "Bob"]
        [Result "1-0"]

        1. e4 e5 2. Nf3 Nc6 3. Bb5 a6 {standard} 4. Ba4 Nf6 5. O-O Be7 1-0
    """.trimIndent()

    @Test
    fun `parses headers and mainline`() {
        val games = PgnParser.parseGames(sample)
        assertEquals(1, games.size)
        val game = games.first()
        assertEquals("Alice", game.white)
        assertEquals("Bob", game.black)
        assertEquals("1-0", game.result)
        assertEquals(10, game.moves.size)
        assertTrue(game.moves.first().san.startsWith("e4"))
        assertEquals("e2e4", game.moves.first().uci)
        assertNotNull(game.moves.first().fenAfter)
    }

    @Test
    fun `handles glued move numbers`() {
        val pgn = "1.e4 e5 2.Nf3 1/2-1/2"
        val games = PgnParser.parseGames(pgn)
        assertEquals(3, games.first().moves.size)
    }

    @Test
    fun `multiple games in one text`() {
        val pgn = """
            1. e4 * 
            
            [Event "Second"]
            1. d4 d5 0-1
        """.trimIndent()
        val games = PgnParser.parseGames(pgn)
        assertEquals(2, games.size)
        assertEquals(1, games[0].moves.size)
        assertEquals(2, games[1].moves.size)
    }

    @Test
    fun `ignores variations and comments`() {
        val pgn = "1. e4 (1. d4 d5) e5 {comment} 2. Nf3 (2. f4) Nc6 1-0"
        val game = PgnParser.parseGames(pgn).first()
        val sans = game.sanMoves
        assertEquals(listOf("e4", "e5", "Nf3", "Nc6"), sans)
    }

    @Test
    fun `handles nag symbols`() {
        val pgn = "1.e4! e5? 2.Nf3!? Nc6!! 1-0"
        val game = PgnParser.parseGames(pgn).first()
        assertEquals(4, game.moves.size)
        assertEquals("e2e4", game.moves[0].uci)
    }

    @Test
    fun `handles black to move fen start`() {
        val pgn = """
            [FEN "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1"]
            [Result "*"]
            
            1... e5 2. Nf3 *
        """.trimIndent()
        val game = PgnParser.parseGames(pgn).first()
        assertEquals("e7e5", game.moves.first().uci)
        assertEquals(2, game.moves.size)
    }

    @Test
    fun `round trips through writer`() {
        val game = PgnParser.parseGames(sample).first()
        val text = PgnWriter.write(game)
        val reparsed = PgnParser.parseGames(text).first()
        assertEquals(game.sanMoves, reparsed.sanMoves)
        assertEquals(game.headers["White"], reparsed.headers["White"])
    }

    @Test
    fun `unknown move stops the mainline`() {
        val pgn = "1. e4 e5 2. Qh5 bogus 1-0"
        val game = PgnParser.parseGames(pgn).first()
        // 3 resolved moves then failure recorded.
        assertEquals(4, game.moves.size)
        assertNotNull(game.moves[2].move)
        // The failed ply carries a null move.
        assertTrue(game.moves.any { it.move == null })
    }
}