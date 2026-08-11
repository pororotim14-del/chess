package com.chessassistant.coreengine.trackers

/**
 * Accuracy of a single move, computed from the engine score before and after
 * the move. The score is always from the mover's perspective so a drop in
 * score reduces accuracy.
 */
data class MoveAccuracy(
    val ply: Int,
    val score: Int,
    val delta: Int,
    val accuracy: Int, // 0..100
    val bestMove: String?,
    val playedMove: String,
)

/**
 * Tracks accuracy for a played game using engine evaluations.
 */
interface GameAccuracyTracker {
    fun record(ply: Int, score: Int, bestMove: String?, playedMove: String): MoveAccuracy
    fun snapshots(): List<MoveAccuracy>
    fun reset()
}

/**
 * Simple accuracy model: a perfect move (best or no score change) is 100,
 * and accuracy drops linearly with the amount of material lost relative to
 * the best move, floor 0.
 */
class DefaultGameAccuracyTracker : GameAccuracyTracker {

    private val history = mutableListOf<MoveAccuracy>()

    override fun record(ply: Int, score: Int, bestMove: String?, playedMove: String): MoveAccuracy {
        val best = history.lastOrNull()
        val delta = best?.let { it.score - score } ?: 0
        val accuracy = accuracyFor(delta, bestMove, playedMove)
        val entry = MoveAccuracy(
            ply = ply,
            score = score,
            delta = delta,
            accuracy = accuracy,
            bestMove = bestMove,
            playedMove = playedMove,
        )
        history += entry
        return entry
    }

    private fun accuracyFor(delta: Int, bestMove: String?, playedMove: String): Int {
        if (playedMove == bestMove) return 100
        // A pawn is worth 100 centipawns; losing more than a queen maps to 0.
        val lost = delta.coerceAtLeast(0)
        val result = (100 - lost / 2).coerceIn(0, 99)
        return if (bestMove == null) 100 else result
    }

    override fun snapshots(): List<MoveAccuracy> = history.toList()

    override fun reset() {
        history.clear()
    }
}