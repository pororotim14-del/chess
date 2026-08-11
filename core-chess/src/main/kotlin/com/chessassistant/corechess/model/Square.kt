package com.chessassistant.corechess.model

/**
 * A square on the board, 0x88-style flat index: `file + rank * 8`.
 *
 * file 0 (a) .. 7 (h), rank 0 (1) .. 7 (8). a1 = 0, h8 = 63.
 */
object Square {
    const val A1 = 0; const val B1 = 1; const val C1 = 2; const val D1 = 3
    const val E1 = 4; const val F1 = 5; const val G1 = 6; const val H1 = 7
    const val A8 = 56; const val E8 = 60; const val H8 = 63

    fun index(file: Int, rank: Int): Int = file + rank * 8

    fun file(sq: Int): Int = sq and 7

    fun rank(sq: Int): Int = sq ushr 3

    /** Algebraic square name, e.g. 52 -> "e4". */
    fun name(sq: Int): String = "${('a'.code + file(sq)).toChar()}${rank(sq) + 1}"

    /** Parse an algebraic square name into an index, or null when invalid/malformed. */
    fun fromName(name: String): Int? {
        if (name.length != 2) return null
        val f = name[0].lowercaseChar()
        val r = name[1]
        if (f !in 'a'..'h' || r !in '1'..'8') return null
        return index(f - 'a', r - '1')
    }

    fun isValid(sq: Int): Boolean = sq in 0..63
}