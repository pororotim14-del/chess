package com.chessassistant.featureassistant.a11y

import com.chessassistant.corechess.model.Move
import com.chessassistant.corechess.model.Position
import com.chessassistant.corechess.notation.FenParser
import com.chessassistant.corechess.notation.San
import com.chessassistant.corechess.rules.MoveGenerator

/**
 * Best-effort parser that reconstructs a chess [Position] from the text an
 * accessibility service can read off the screen of another app.
 *
 * Strategy:
 *  1. Look for a literal FEN string.
 *  2. Otherwise tokenize the text as SAN moves and replay them from the
 *     standard start position.
 *  3. Otherwise try UCI move strings.
 */
object ChessTextParser {

    private val FEN_REGEX = Regex(
        "\\b([rnbqkbnrpPRNBQK1-8]{1,8})(?:/([rnbqkbnrpPRNBQK1-8]{1,8})){7}" +
            "\\s+([wb])\\s+([KQkq-]{1,4})\\s+([a-h][1-8]|-)(?:\\s+(\\d+)\\s+(\\d+))?\\b",
    )

    private val SAN_TOKEN = Regex(
        "(?:0-0-0|0-0|O-O-O|O-O|[KQRBNP]?[a-h]?[1-8]?x?[a-h][1-8](?:=[QRBN])?[+#]?[!?]*)",
    )

    private val MOVE_NUMBER = Regex("^\\d+\\.*$")
    private val UCI_TOKEN = Regex("^[a-h][1-8][a-h][1-8][qrbn]?$")

    private val RESULT_TOKEN = Regex("^(1-0|0-1|1/2-1/2|1\\/2|0-1)$")

    data class ParseResult(
        val fen: String? = null,
        val moves: List<String> = emptyList(),
        val hints: List<String> = emptyList(),
    ) {
        val detected: Boolean get() = fen != null
    }

    fun parse(texts: List<String>): ParseResult {
        val joined = texts.joinToString(" ").replace('\n', ' ')

        FEN_REGEX.find(joined)?.let { match ->
            val fen = match.value.trim()
            if (FenParser.parse(fen) != null) {
                return ParseResult(
                    fen = fen,
                    hints = listOf("FEN terdeteksi"),
                )
            }
        }

        val sans = tokenizeSans(joined)
        if (sans.isNotEmpty()) {
            positionFromMoves(sans)?.let { pos ->
                return ParseResult(
                    fen = pos.toFen(),
                    moves = sans,
                    hints = listOf("Notasi SAN terdeteksi"),
                )
            }
        }

        val ucis = joined.split(Regex("\\s+")).filter { UCI_TOKEN.matches(it) }
        if (ucis.isNotEmpty()) {
            positionFromUcis(ucis)?.let { pos ->
                return ParseResult(
                    fen = pos.toFen(),
                    moves = ucis,
                    hints = listOf("Notasi UCI terdeteksi"),
                )
            }
        }

        val hints = mutableListOf<String>()
        if (joined.contains("checkmate", ignoreCase = true)) hints += "Checkmate"
        if (joined.contains("check", ignoreCase = true)) hints += "Check"
        joined.split(Regex("\\s+")).forEach { if (RESULT_TOKEN.matches(it)) hints += "Hasil: $it" }
        if (hints.isEmpty()) hints += "Tidak ada posisi terdeteksi"
        return ParseResult(hints = hints)
    }

    fun tokenizeSans(text: String): List<String> {
        val words = text.split(Regex("\\s+"))
        val out = mutableListOf<String>()
        for (w in words) {
            var t = w.trim()
            if (t.isEmpty()) continue
            if (t == "e.p." || t == ".e.p.") continue
            Regex("^\\d+\\.+").find(t)?.let { t = t.removePrefix(it.value) }
            t = t.trimEnd('!', '?', ';')
            if (t.isEmpty()) continue
            if (MOVE_NUMBER.matches(t)) continue
            if (SAN_TOKEN.matches(t)) out += t
        }
        return out
    }

    fun positionFromMoves(moves: List<String>): Position? {
        var pos = Position.STARTING
        for (san in moves) {
            val move = San.parse(pos, san) ?: return null
            pos = pos.apply(move)
        }
        return pos
    }

    fun positionFromUcis(ucis: List<String>): Position? {
        var pos = Position.STARTING
        for (uci in ucis) {
            val move = Move.fromUci(uci) ?: return null
            val legal = MoveGenerator.legalMoves(pos).firstOrNull { it == move } ?: return null
            pos = pos.apply(legal)
        }
        return pos
    }
}
