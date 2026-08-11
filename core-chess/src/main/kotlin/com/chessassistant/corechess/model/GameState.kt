package com.chessassistant.corechess.model

import com.chessassistant.corechess.rules.GameStatus
import com.chessassistant.corechess.rules.MoveGenerator

/**
 * Interactive game session over a board with undo/redo.
 * The position is rebuilt by replaying move history, which keeps the model
 * simple and correct at the cost of small O(game length) work per step.
 */
class GameState(initialFen: String = Position.START_FEN) {

    private val startPos: Position =
        com.chessassistant.corechess.notation.FenParser.parse(initialFen)
            ?: Position.STARTING

    private val history = ArrayList<Move>()
    private var cursor = 0

    var position: Position = startPos
        private set

    val startFen: String get() = startPos.toFen()

    /** Moves played so far, in order (up to the current cursor). */
    fun playedMoves(): List<Move> = history.take(cursor)

    /** The move that produced the current position, if any (for last-move highlight). */
    val lastMove: Move?
        get() = if (cursor > 0) history[cursor - 1] else null

    fun legalMoves(): List<Move> = MoveGenerator.legalMoves(position)

    fun gameStatus(): GameStatus = MoveGenerator.gameStatus(position)

    val canUndo: Boolean get() = cursor > 0

    val canRedo: Boolean get() = cursor < history.size

    /** Attempts to play [move] if it is legal for the current position. */
    fun play(move: Move): Boolean {
        if (MoveGenerator.legalMoves(position).none { it == move }) return false
        if (cursor < history.size) {
            // Collapse a previously undone branch.
            while (history.size > cursor) history.removeAt(history.lastIndex)
        }
        history.add(move)
        cursor++
        position = rebuild(cursor)
        return true
    }

    fun playSan(san: String): Boolean {
        val move = com.chessassistant.corechess.notation.San.parse(position, san) ?: return false
        return play(move)
    }

    fun playUci(uci: String): Boolean {
        val move = Move.fromUci(uci) ?: return false
        return play(move)
    }

    fun undo(): Boolean {
        if (!canUndo) return false
        cursor--
        position = rebuild(cursor)
        return true
    }

    fun redo(): Boolean {
        if (!canRedo) return false
        cursor++
        position = rebuild(cursor)
        return true
    }

    fun reset() {
        cursor = 0
        history.clear()
        position = startPos
    }

    private fun rebuild(count: Int): Position {
        var pos = startPos
        for (i in 0 until count) pos = pos.apply(history[i])
        return pos
    }

    /** Reconstructs the position at an arbitrary ply without touching cursor. */
    fun positionAt(ply: Int): Position {
        val capped = ply.coerceIn(0, cursor)
        return rebuild(capped)
    }

    /** Returns the current position as FEN. */
    fun fen(): String = position.toFen()
}