package com.chessassistant.nativeengine

/**
 * Thin façade over the C++ native engine (Stockfish).
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

    /**
     * Detailed analysis of a position with configurable depth.
     * Returns AnalysisResult with best move, evaluation, and principal variation.
     */
    @JvmStatic
    external fun analyzePosition(fen: String, depth: Int): AnalysisResult

    /**
     * Sets the search depth for subsequent bestMove/evalSummary calls.
     */
    @JvmStatic
    external fun setSearchDepth(depth: Int)

    /**
     * Gets the principal variation for the last analyzed position.
     */
    @JvmStatic
    external fun getPrincipalVariation(): String

    /**
     * Security: Validates engine integrity at runtime.
     * Returns true if engine binary matches expected checksum.
     */
    @JvmStatic
    external fun verifyEngineIntegrity(): Boolean

    /**
     * Security: Gets engine fingerprint for attestation.
     */
    @JvmStatic
    external fun getEngineFingerprint(): String

    data class AnalysisResult(
        val bestMove: String,
        val evaluation: Int,
        val principalVariation: String,
        val depth: Int,
        val nodes: Long,
        val timeMs: Long
    )
}
