package com.chessassistant.coreengine.analysis

/**
 * Lifecycle of the engine backend running on the device.
 */
sealed interface EngineState {
    data object Idle : EngineState
    data object Searching : EngineState
    data object Paused : EngineState
    data object Ready : EngineState
    data class Stopped(val reason: String) : EngineState
    data class Failed(val reason: String) : EngineState
}