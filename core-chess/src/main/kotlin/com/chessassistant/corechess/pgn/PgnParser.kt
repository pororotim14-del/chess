package com.chessassistant.corechess.pgn

import com.chessassistant.corechess.notation.FenParser
import com.chessassistant.corechess.notation.San
import com.chessassistant.corechess.model.Position

/**
 * Structurally-tolerant PGN parser. Handles:
 *  - multiple games in a single file
 *  - header blocks `[Key "Value"]`
 *  - move numbers `N.` / `N...` (also glued: `1.e4`)
 *  - NAGs `$4`, `!`, `?`, `!?`
 *  - comments `{...}` and `;...`
 *  - variations `(...)` (ignored for the mainline)
 *  - results `1-0`, `0-1`, `1/2-1/2`, `*`
 */
object PgnParser {

    fun parseGames(text: String): List<PgnGame> {
        val tokens = tokenize(text)
        val games = ArrayList<PgnGame>()
        var i = 0
        val n = tokens.size

        while (i < n) {
            // Optional header block.
            val headers = LinkedHashMap<String, String>()
            while (i < n && tokens[i].kind == TokenKind.HEADER_LINE) {
                val parts = tokens[i].text.split('\n', limit = 2)
                val body = parts.first().trim()
                val eq = body.indexOf(' ')
                if (eq > 0) {
                    val key = body.substring(0, eq).trim()
                    val value = body.substring(eq).trim().removeSurrounding("\"")
                    headers[key] = value
                }
                i++
            }

            if (i < n && tokens[i].kind == TokenKind.SAN) {
                val sanTokens = ArrayList<Token>()
                var result: String? = null
                while (i < n && tokens[i].kind != TokenKind.HEADER_LINE) {
                    val t = tokens[i]
                    when (t.kind) {
                        TokenKind.SAN -> sanTokens.add(t)
                        TokenKind.RESULT -> result = t.text
                        else -> Unit
                    }
                    i++
                }
                if (sanTokens.isNotEmpty() || result != null) {
                    val startFen = headers["FEN"]?.let { FenParser.parse(it)?.toFen() }
                        ?: Position.START_FEN
                    val records = PgnResolver.resolve(sanTokens.map { it.text }, startFen)
                    games.add(
                        PgnGame(
                            headers = headers,
                            moves = records,
                            result = result,
                            startFen = startFen,
                        ),
                    )
                }
            } else {
                i++
            }
        }
        return games
    }

    private enum class TokenKind { HEADER_LINE, SAN, RESULT }

    private data class Token(val kind: TokenKind, val text: String)

    private fun tokenize(text: String): List<Token> {
        val tokens = ArrayList<Token>()
        var i = 0
        val n = text.length
        while (i < n) {
            val c = text[i]
            when {
                c.isWhitespace() -> i++
                c == '[' -> {
                    i++
                    val sb = StringBuilder()
                    var closed = false
                    while (i < n) {
                        if (text[i] == ']') {
                            closed = true
                            i++
                            break
                        }
                        sb.append(text[i])
                        i++
                    }
                    if (closed) tokens.add(Token(TokenKind.HEADER_LINE, sb.toString().trim()))
                }
                c == '{' -> {
                    i++
                    while (i < n && text[i] != '}') i++
                    i++
                }
                c == ';' -> {
                    while (i < n && text[i] != '\n') i++
                }
                c == '(' -> {
                    // Skip variation subtree.
                    var depth = 1
                    i++
                    while (i < n && depth > 0) {
                        when (text[i]) {
                            '(' -> depth++
                            ')' -> depth--
                            '{' -> {
                                i++
                                while (i < n && text[i] != '}') i++
                            }
                        }
                        i++
                    }
                }
                c == ')' -> i++
                c == '$' -> {
                    i++
                    while (i < n && text[i].isDigit()) i++
                }
                else -> {
                    val sb = StringBuilder()
                    while (i < n && !text[i].isWhitespace() &&
                        text[i] !in "[]{}();$"
                    ) {
                        sb.append(text[i])
                        i++
                    }
                    val raw = sb.toString()
                    when {
                        raw == "1-0" || raw == "0-1" || raw == "1/2-1/2" || raw == "*" ->
                            tokens.add(Token(TokenKind.RESULT, raw))
                        raw == "..." -> Unit
                        else -> tokens.add(Token(TokenKind.SAN, raw))
                    }
                }
            }
        }
        return tokens
    }

    /**
     * Strips a leading move number from a movetext glyph:
     *  "1." -> "", "1..." -> "", "1.e4" -> "e4", "2.Nf3" -> "Nf3".
     * Glyphs without a numeric prefix (SAN, castles, results) pass through.
     */
    fun stripMoveNumber(glyph: String): String {
        var i = 0
        while (i < glyph.length && glyph[i].isDigit()) i++
        if (i > 0 && i < glyph.length && glyph[i] == '.') {
            i++
            while (i < glyph.length && glyph[i] == '.') i++
            return glyph.substring(i)
        }
        return glyph
    }
}