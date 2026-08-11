package com.chessassistant.featureassistant.a11y

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.pm.PackageManager
import android.graphics.Path
import android.graphics.PointF
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.chessassistant.corechess.model.Move
import com.chessassistant.corechess.model.Position
import com.chessassistant.corechess.model.Square
import com.chessassistant.corechess.notation.FenParser
import com.chessassistant.featureassistant.assistant.AssistantState
import com.chessassistant.nativeengine.NativeEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Reads the visible board position of whichever chess app is in the
 * foreground and, when AI auto-play is enabled, taps the engine's move on
 * the board using [dispatchGesture]. Only active while the assistant is
 * running, and only for windows that are not ours.
 */
class ChessAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var parseRunnable: Runnable? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        scope.cancel()
        instance = null
        super.onDestroy()
    }

    override fun onInterrupt() = Unit

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.packageName?.toString() == packageName) return
        val type = event.eventType
        if (type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            type != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
            type != AccessibilityEvent.TYPE_WINDOWS_CHANGED
        ) {
            return
        }
        if (!AssistantState.running.value) return

        parseRunnable?.let(mainHandler::removeCallbacks)
        val runnable = Runnable { parseAndPublish(event) }
        parseRunnable = runnable
        mainHandler.postDelayed(runnable, 250)
    }

    private fun parseAndPublish(event: AccessibilityEvent) {
        if (!AssistantState.running.value) return
        val root = rootInActiveWindow ?: return
        val texts = mutableListOf<String>()
        collectTexts(root, texts, 0, 800)

        val result = ChessTextParser.parse(texts)
        val source = packageLabel(event.packageName?.toString())
        if (result.detected) {
            AssistantState.setPosition(
                fen = result.fen,
                moves = result.moves,
                source = source,
                hint = result.hints.joinToString(", "),
            )
            maybeAutoPlay(result.fen!!)
        } else {
            AssistantState.setPosition(
                fen = null,
                moves = result.moves,
                source = source,
                hint = result.hints.joinToString(", "),
            )
        }
    }

    private fun maybeAutoPlay(fen: String) {
        if (!AssistantState.running.value || !AssistantState.autoPlay.value) return
        if (AssistantState.boardRect.value == null) {
            AssistantState.setMessage("Aktifkan kalibrasi board untuk auto-play AI")
            return
        }
        val pos = FenParser.parse(fen) ?: return
        if (pos.sideToMove != AssistantState.engineColor.value) return
        if (AssistantState.lastTapFen.value == fen) return
        scheduleAutoMove(pos, force = false)
    }

    /** Public entry point used by the overlay "Jalankan AI" button. */
    fun playBestMove(force: Boolean) {
        val fen = AssistantState.fen.value ?: return
        val pos = FenParser.parse(fen) ?: return
        if (!force) {
            if (pos.sideToMove != AssistantState.engineColor.value) return
            if (AssistantState.lastTapFen.value == fen) return
        }
        scheduleAutoMove(pos, force)
    }

    private fun scheduleAutoMove(pos: Position, force: Boolean) {
        scope.launch {
            delay(650)
            val best = resolveBestMove(pos) ?: run {
                AssistantState.setMessage("Tidak ada langkah AI yang valid")
                return@launch
            }
            val move = Move.fromUci(best) ?: return@launch
            if (pos.applyUci(best) == null && !force) return@launch
            val rect = AssistantState.boardRect.value ?: return@launch
            val fromP = squareCenter(move.from, rect)
            val toP = squareCenter(move.to, rect)
            dispatchTap(fromP)
            mainHandler.postDelayed({ dispatchTap(toP) }, 300)
            AssistantState.markAutoTap(pos.toFen())
            AssistantState.setMessage("AI memainkan ${move.uci}")
        }
    }

    private suspend fun resolveBestMove(pos: Position): String? {
        val current = AssistantState.analysis.value.bestMove
        if (current.isNotEmpty() && pos.applyUci(current) != null) return current
        return withContext(Dispatchers.Default) {
            NativeEngine.bestMove(pos.toFen()).takeIf { pos.applyUci(it) != null }
        }
    }

    private fun dispatchTap(p: PointF) {
        val path = Path().apply { moveTo(p.x, p.y) }
        val stroke = GestureDescription.StrokeDescription(path, 0, 70)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        dispatchGesture(gesture, null, null)
    }

    private fun squareCenter(sq: Int, rect: Rect): PointF {
        val file = Square.file(sq)
        val rank = Square.rank(sq)
        val cellX = rect.width() / 8f
        val cellY = rect.height() / 8f
        val flipped = AssistantState.boardFlipped.value
        val fx = if (!flipped) file else 7 - file
        val fy = if (!flipped) (7 - rank) else rank
        return PointF(
            rect.left + (fx + 0.5f) * cellX,
            rect.top + (fy + 0.5f) * cellY,
        )
    }

    private fun collectTexts(
        node: AccessibilityNodeInfo?,
        out: MutableList<String>,
        depth: Int,
        max: Int,
    ) {
        if (node == null || depth > 6 || out.size >= max) return
        node.text?.toString()?.takeIf { it.isNotBlank() }?.let { out += it }
        node.contentDescription?.toString()?.takeIf { it.isNotBlank() }?.let { out += it }
        for (i in 0 until node.childCount) {
            if (out.size >= max) break
            val child = node.getChild(i) ?: continue
            collectTexts(child, out, depth + 1, max)
        }
    }

    private fun packageLabel(pkg: String?): String {
        if (pkg == null) return "?"
        return try {
            val pm = packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            pkg
        }
    }

    companion object {
        @Volatile
        private var instance: ChessAccessibilityService? = null

        /** Called from the overlay panel to force the AI move right now. */
        @JvmStatic
        fun playBestMoveNow() {
            instance?.playBestMove(force = true)
        }
    }
}
