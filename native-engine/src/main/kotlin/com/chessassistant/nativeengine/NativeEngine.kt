package com.chessassistant.nativeengine

/**
 * Thin façade over the C++ native engine.
 */
object NativeEngine {

    init {
        System.loadLibrary("chessengine")
    }

    /** Incremented when the JNI signature changes. */
    @JvmStatic
    external fun bindingVersion(): Int

    /**
     * Static evaluation of the given FEN in centipawns from White's view.
     * Returns 0 when the FEN cannot be parsed.
     */
    @JvmStatic
    external fun evalSummary(fen: String): Int

    /**
     * Best move for the side to move in the given FEN, in UCI notation
     * (e.g. "e2e4", "a7a8q"). Returns "" when the FEN is invalid or no
     * legal move exists.
     */
    @JvmStatic
    external fun bestMove(fen: String): String
}
