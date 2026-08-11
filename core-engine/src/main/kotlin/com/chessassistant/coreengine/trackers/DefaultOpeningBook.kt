package com.chessassistant.coreengine.trackers

/**
 * Moves are stored as SAN strings because they are short and human readable;
 * the software binder applies them to a fresh board to normalize them to UCI.
 */
class DefaultOpeningBook : OpeningBook {

    private val book: List<Opening> =
        listOf(
            Opening("Italian Game", "C50", listOf("e4", "e5", "Nf3", "Nc6", "Bc4")),
            Opening("Ruy Lopez", "C60", listOf("e4", "e5", "Nf3", "Nc6", "Bb5")),
            Opening("Scotch Game", "C45", listOf("e4", "e5", "Nf3", "Nc6", "d4")),
            Opening("Sicilian Defense", "B20", listOf("e4", "c5")),
            Opening("Sicilian: Open", "B30", listOf("e4", "c5", "Nf3", "Nc6", "d4", "cxd4", "Nxd4")),
            Opening("French Defense", "C00", listOf("e4", "e6")),
            Opening("Caro-Kann Defense", "B10", listOf("e4", "c6")),
            Opening("King's Pawn Game", "C20", listOf("e4", "e5")),
            Opening("Queen's Gambit", "D06", listOf("d4", "d5", "c4")),
            Opening("King's Indian Defense", "E60", listOf("d4", "Nf6", "c4", "g6")),
            Opening("Queen's Indian Defense", "E12", listOf("d4", "Nf6", "c4", "e6", "Nf3", "b6")),
            Opening("English Opening", "A20", listOf("c4")),
            Opening("Reti Opening", "A09", listOf("Nf3", "d5", "g3")),
        )

    override fun find(playedMoves: List<String>): Opening? =
        book
            .filter { matchesPrefix(it.moves, playedMoves) }
            .maxByOrNull { it.moves.size }

    override fun allOpenings(): List<Opening> = book.toList()

    private fun matchesPrefix(line: List<String>, played: List<String>): Boolean {
        if (played.isEmpty()) return false
        val common = minOf(line.size, played.size)
        return played.take(common) == line.take(common)
    }
}