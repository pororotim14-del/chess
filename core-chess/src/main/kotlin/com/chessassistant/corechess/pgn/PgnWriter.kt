package com.chessassistant.corechess.pgn

import com.chessassistant.corechess.model.Color
import com.chessassistant.corechess.model.Position

/** Serializes a parsed game back to portable PGN text. */
object PgnWriter {

    private val DEFAULT_HEADERS =
        listOf("Event", "Site", "Date", "Round", "White", "Black", "Result")

    fun write(game: PgnGame): String {
        val sb = StringBuilder()

        val headerKeys = LinkedHashSet<String>()
        headerKeys.addAll(DEFAULT_HEADERS)
        headerKeys.addAll(game.headers.keys)

        for (key in headerKeys) {
            if (key == "FEN") continue
            val value = when (key) {
                "Result" -> game.result ?: game.headers["Result"] ?: PgnResult.OPEN
                else -> game.headers[key] ?: ""
            }
            if (value.isBlank()) continue
            sb.append('[').append(key).append(" \"")
                .append(value.replace("\"", "'"))
                .append("\"]\n")
        }
        if (game.startFen != Position.START_FEN) {
            sb.append("[FEN \"").append(game.startFen).append("\"]\n")
        }

        sb.append('\n')

        var column = 0
        for (record in game.moves) {
            val token = if (record.color == Color.WHITE) {
                "${record.moveNumber}. ${record.san}"
            } else if (record.plyIndex == 1) {
                // Black starts the game (from a non-start FEN): number it.
                "${record.moveNumber}... ${record.san}"
            } else {
                record.san
            }
            if (column + token.length + 1 > 80 && column > 0) {
                sb.append('\n')
                column = 0
            }
            if (column > 0) {
                sb.append(' ')
                column++
            }
            sb.append(token)
            column += token.length
        }

        val result = game.result ?: game.headers["Result"] ?: PgnResult.OPEN
        sb.append(' ').append(result)
        return sb.toString().trimEnd()
    }
}