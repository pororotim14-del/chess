package com.chessassistant.featureassistant.overlay

import com.chessassistant.corechess.notation.FenParser
import com.chessassistant.featureassistant.assistant.AssistantState
import com.chessassistant.nativeengine.NativeEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Drives the native engine over the latest detected position and publishes
 * live analysis (best move, evaluation, PV) into [AssistantState].
 *
 * The native engine keeps a single internal position, so every call is
 * serialized with a mutex and the newest FEN wins via collectLatest.
 */
object AssistantAnalyzer {

    private val mutex = Mutex()

    fun launch(scope: CoroutineScope) {
        scope.launch {
            AssistantState.fen.collectLatest { fen ->
                if (fen == null || !AssistantState.running.value) {
                    AssistantState.setAnalysis(AssistantState.Analysis())
                    return@collectLatest
                }
                val analysis = withContext(Dispatchers.Default) {
                    mutex.withLock {
                        runCatching {
                            val best = NativeEngine.bestMove(fen)
                            val eval = NativeEngine.evalSummary(fen)
                            AssistantState.Analysis(
                                bestMove = best,
                                evalCp = eval,
                                pv = buildPv(fen, best),
                                updatedAt = System.currentTimeMillis(),
                            )
                        }.getOrNull()
                    }
                }
                if (analysis != null) AssistantState.setAnalysis(analysis)
            }
        }
    }

    private fun buildPv(fen: String, best: String): String {
        if (best.isEmpty()) return ""
        val start = FenParser.parse(fen) ?: return ""
        val line = mutableListOf(best)
        var pos = start.applyUci(best) ?: return best
        repeat(3) {
            val next = NativeEngine.bestMove(pos.toFen())
            if (next.isEmpty() || pos.applyUci(next) == null) return@repeat
            line += next
            pos = pos.applyUci(next)!!
        }
        return line.joinToString(" ")
    }
}
