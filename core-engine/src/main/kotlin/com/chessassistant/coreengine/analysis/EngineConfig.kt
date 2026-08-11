package com.chessassistant.coreengine.analysis

/**
 * Configuration for a single [AnalysisEngine] instance.
 */
data class EngineConfig(
    val depth: Int = 18,
    val threads: Int = 1,
    val hashMb: Int = 32,
) {
    companion object {
        val DEFAULT = EngineConfig()
    }
}