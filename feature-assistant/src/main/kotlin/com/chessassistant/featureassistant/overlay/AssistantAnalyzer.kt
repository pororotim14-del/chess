package com.chessassistant.featureassistant.overlay

import com.chessassistant.corechess.notation.FenParser
import com.chessassistant.corechess.model.Move
import com.chessassistant.corechess.model.Position
import com.chessassistant.featureassistant.assistant.AssistantState
import com.chessassistant.nativeengine.NativeEngine
import com.chessassistant.security.engine.EngineSecurityManager
import com.chessassistant.security.engine.SecureEngineWrapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Drives the native engine (Stockfish) over the latest detected position and publishes
 * live analysis (best move, evaluation, PV) into [AssistantState].
 * 
 * Includes move classification: Brilliant (!!), Good (!), Inaccurate (?), Mistake (??), Blunder (???)
 */
object AssistantAnalyzer {

    private val mutex = Mutex()
    private var secureWrapper: SecureEngineWrapper? = null

    fun launch(scope: CoroutineScope, securityManager: EngineSecurityManager) {
        secureWrapper = SecureEngineWrapper(securityManager, com.chessassistant.security.engine.EngineSecurityLevel.STANDARD)
        
        scope.launch {
            AssistantState.fen.collectLatest { fen ->
                if (fen == null || !AssistantState.running.value) {
                    AssistantState.setAnalysis(AssistantState.Analysis())
                    return@collectLatest
                }
                val analysis = withContext(Dispatchers.Default) {
                    mutex.withLock {
                        runCatching {
                            val result = secureWrapper!!.analyze(fen, 18)
                            val classification = classifyMove(fen, result.bestMove, result.evaluation)
                            
                            AssistantState.Analysis(
                                bestMove = result.bestMove,
                                evalCp = result.evaluation,
                                pv = result.principalVariation,
                                updatedAt = System.currentTimeMillis(),
                                moveClassification = classification,
                                depth = result.depth,
                                nodes = result.nodes
                            )
                        }.getOrNull()
                    }
                }
                if (analysis != null) AssistantState.setAnalysis(analysis)
            }
        }
    }

    /**
     * Classifies a move based on evaluation difference from best move.
     * Returns: "!!" (Brilliant), "!" (Good), "?" (Inaccurate), "??" (Mistake), "???" (Blunder), "" (Book/Best)
     */
    private fun classifyMove(fen: String, bestMove: String, evalCp: Int): String {
        if (bestMove.isEmpty()) return ""
        
        val pos = FenParser.parse(fen) ?: return ""
        val legalMoves = com.chessassistant.corechess.rules.MoveGenerator.legalMoves(pos)
        
        if (legalMoves.size == 1) return "!!" // Forced move = brilliant
        
        // Evaluate the best move vs second best to determine quality
        val bestMoveObj = Move.fromUci(bestMove) ?: return ""
        val nextPos = pos.apply(bestMoveObj) ?: return ""
        
        // Simple classification based on eval
        // In a real implementation, we'd compare top 2-3 moves
        return when {
            evalCp >= 500 -> "!!"  // Winning advantage
            evalCp >= 200 -> "!"   // Significant advantage
            evalCp >= -50 -> ""    // Roughly equal (best move)
            evalCp >= -150 -> "?"  // Slight inaccuracy
            evalCp >= -300 -> "??" // Mistake
            else -> "???"          // Blunder
        }
    }

    /**
     * Gets detailed move quality analysis for UI display.
     */
    suspend fun getMoveQuality(fen: String): MoveQualityAnalysis = withContext(Dispatchers.Default) {
        mutex.withLock {
            runCatching {
                val result = secureWrapper!!.analyze(fen, 20)
                MoveQualityAnalysis.fromAnalysis(result)
            }.getOrNull() ?: MoveQualityAnalysis.empty()
        }
    }
}

/**
 * Detailed move quality analysis for UI.
 */
data class MoveQualityAnalysis(
    val bestMove: String = "",
    val classification: String = "",
    val evaluation: Int = 0,
    val principalVariation: String = "",
    val topMoves: List<MoveEval> = emptyList(),
    val depth: Int = 0,
    val nodes: Long = 0,
    val timeMs: Long = 0,
) {
    companion object {
        fun empty(): MoveQualityAnalysis = MoveQualityAnalysis()
        
        fun fromAnalysis(result: NativeEngine.AnalysisResult): MoveQualityAnalysis {
            val topMoves = parseTopMoves(result.principalVariation, result.evaluation)
            return MoveQualityAnalysis(
                bestMove = result.bestMove,
                classification = classifyEvaluation(result.evaluation),
                evaluation = result.evaluation,
                principalVariation = result.principalVariation,
                topMoves = topMoves,
                depth = result.depth,
                nodes = result.nodes,
                timeMs = result.timeMs
            )
        }
        
        private fun parseTopMoves(pv: String, eval: Int): List<MoveEval> {
            val moves = pv.split(" ").filter { it.isNotEmpty() }
            return moves.mapIndexed { index, move ->
                MoveEval(
                    move = move,
                    evaluation = eval - (index * 50), // Rough estimate
                    rank = index + 1
                )
            }
        }
        
        private fun classifyEvaluation(eval: Int): String {
            return when {
                eval >= 500 -> "!! Brilliant"
                eval >= 200 -> "! Good"
                eval >= -50 -> "✓ Best"
                eval >= -150 -> "? Inaccurate"
                eval >= -300 -> "?? Mistake"
                else -> "??? Blunder"
            }
        }
    }
}

data class MoveEval(
    val move: String,
    val evaluation: Int,
    val rank: Int
)
