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
     * Request a full evaluation from the given position. The result is a
     * crude centipawn summary for now; the real multi-ply search is planned
     * behind this call.
     */
    @JvmStatic
    external fun evalSummary(fen: String): Int
}