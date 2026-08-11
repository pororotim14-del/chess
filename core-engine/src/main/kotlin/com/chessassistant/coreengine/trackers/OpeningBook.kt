package com.chessassistant.coreengine.trackers

/**
 * A chess opening. [name] is the human-readable name and [moves] holds the
 * main-line moves in UCI (or SAN) order used to match a played game.
 */
data class Opening(
    val name: String,
    val eco: String,
    val moves: List<String>,
)

/**
 * Matches played move sequences against a small bundled opening book.
 */
interface OpeningBook {
    /**
     * Returns the opening that best matches [playedMoves] (longest prefix),
     * or `null` when the game has left the book.
     */
    fun find(playedMoves: List<String>): Opening?

    fun allOpenings(): List<Opening>
}