package com.chessassistant.corechess.model

/** The two chess colors. */
enum class Color {
    WHITE, BLACK;

    val opposite: Color
        get() = if (this == WHITE) BLACK else WHITE

    val fenSymbol: Char
        get() = if (this == WHITE) 'w' else 'b'
}