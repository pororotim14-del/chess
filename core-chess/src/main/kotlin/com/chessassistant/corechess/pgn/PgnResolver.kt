package com.chessassistant.corechess.pgn

import com.chessassistant.corechess.notation.FenParser
import com.chessassistant.corechess.notation.San
import com.chessassistant.corechess.model.Move
import com.chessassistant.corechess.model.Position

/** Resolves SAN tokens into concrete moves against a real board. */
object PgnResolver {

    /**
     * Plays a mainline of SAN glyphs from [startFen].
     * Unresolvable ply stop the game; [PgnMoveRecord.move] is null for a
     * failed ply and all later plies are dropped.
     */
    fun resolve(glyphs: List<String>, startFen: String): List<PgnMoveRecord> {
        val pos = FenParser.parse(startFen) ?: return emptyList()
        if (glyphs.isEmpty()) return emptyList()

        val records = ArrayList<PgnMoveRecord>(glyphs.size)
        var cursor: Position = pos
        var ply = 0
        for (g in glyphs) {
            val san = San.normalize(PgnParser.stripMoveNumber(g))
            // "..." alone after a number means black-to-move continuation; skip.
            if (san.isEmpty()) continue

            val move: Move? = San.parse(cursor, san)
            if (move == null) {
                records.add(
                    PgnMoveRecord(
                        san = PgnParser.stripMoveNumber(g),
                        move = null,
                        color = cursor.sideToMove,
                        plyIndex = ply,
                        moveNumber = cursor.fullmoveNumber,
                        fenAfter = null,
                    ),
                )
                break
            }

            val next = cursor.apply(move)
            records.add(
                PgnMoveRecord(
                    san = g, // keep the raw san text for the PGN view
                    move = move,
                    color = cursor.sideToMove,
                    plyIndex = ply,
                    moveNumber = cursor.fullmoveNumber,
                    fenAfter = next.toFen(),
                ),
            )
            cursor = next
            ply++
        }
        return records
    }
}