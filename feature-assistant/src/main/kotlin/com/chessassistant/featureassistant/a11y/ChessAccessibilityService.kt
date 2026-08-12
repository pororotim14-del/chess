package com.chessassistant.featureassistant.a11y

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ColorSpace
import android.graphics.Path
import android.graphics.PointF
import android.graphics.Rect
import android.hardware.HardwareBuffer
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
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
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy
import java.util.HashMap
import java.util.concurrent.Executor

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

    private val readers = HashMap<String, ScreenBoardReader>()
    private var lastScreenCapture = 0L

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
        val pkg = event.packageName?.toString()
        val source = packageLabel(pkg)
        val root = rootInActiveWindow
        if (root != null) {
            val texts = mutableListOf<String>()
            collectTexts(root, texts, 0, 800)
            val result = ChessTextParser.parse(texts)
            publish(result.fen, result.moves, source, result.hints.joinToString(", "))
        } else {
            publish(null, emptyList(), source, "Menunggu deteksi posisi...")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            maybeReadScreen(pkg, source)
        }
    }

    private fun publish(fen: String?, moves: List<String>, source: String?, hint: String) {
        AssistantState.setPosition(fen, moves, source, hint)
        if (fen != null) maybeAutoPlay(fen)
    }

    /** Best-effort: also read the board straight from screen pixels. */
    @RequiresApi(Build.VERSION_CODES.R)
    private fun maybeReadScreen(pkg: String?, source: String?) {
        if (!AssistantState.running.value) return
        val now = SystemClock.uptimeMillis()
        if (now - lastScreenCapture < SCREEN_CAPTURE_INTERVAL_MS) return
        lastScreenCapture = now
        val key = pkg ?: "?"
        captureScreenshotCompat(
            executor = ContextCompat.getMainExecutor(this),
            onSuccess = { buffer, colorSpace -> handleScreenshot(buffer, colorSpace, key, source) },
            onFailure = {},
        )
    }

    /**
     * Calls AccessibilityService.takeScreenshot via reflection: the result
     * type was renamed from TakeScreenshotResult (API 30-33) to
     * ScreenshotResult (API 34+) while the method and callback interface kept
     * their names, so reflection works across every supported API level.
     */
    private fun captureScreenshotCompat(
        executor: Executor,
        onSuccess: (HardwareBuffer, ColorSpace?) -> Unit,
        onFailure: () -> Unit,
    ) {
        try {
            val callbackClass = Class.forName("android.accessibilityservice.AccessibilityService\$TakeScreenshotCallback")
            val resultClass = try {
                Class.forName("android.accessibilityservice.AccessibilityService\$ScreenshotResult")
            } catch (e: ClassNotFoundException) {
                Class.forName("android.accessibilityservice.AccessibilityService\$TakeScreenshotResult")
            }
            val method = AccessibilityService::class.java.getMethod(
                "takeScreenshot",
                Int::class.javaPrimitiveType,
                Executor::class.java,
                callbackClass,
            )
            val handler = InvocationHandler { _, invoked, args ->
                when (invoked.name) {
                    "onSuccess" -> {
                        val result = args?.get(0) ?: return@InvocationHandler null
                        val buffer = result.javaClass.getMethod("getHardwareBuffer")
                            .invoke(result) as HardwareBuffer
                        val colorSpace = result.javaClass.getMethod("getColorSpace")
                            .invoke(result) as? ColorSpace
                        onSuccess(buffer, colorSpace)
                        null
                    }
                    "onFailure" -> {
                        onFailure()
                        null
                    }
                    else -> null
                }
            }
            val callback = Proxy.newProxyInstance(
                callbackClass.classLoader,
                arrayOf(callbackClass),
                handler,
            )
            method.invoke(this, Display.DEFAULT_DISPLAY, executor, callback)
        } catch (e: Exception) {
            // Screenshot capture unavailable — text detection still runs.
        }
    }

    private fun handleScreenshot(buffer: HardwareBuffer, colorSpace: ColorSpace?, key: String, source: String?) {
        val bmp = Bitmap.wrapHardwareBuffer(buffer, colorSpace) ?: run {
            buffer.close()
            return
        }
        buffer.close()
        val w = bmp.width
        val h = bmp.height
        val px = IntArray(w * h)
        bmp.getPixels(px, 0, w, 0, 0, w, h)
        bmp.recycle()

        scope.launch {
            val reader = readers.getOrPut(key) { ScreenBoardReader() }
            val detection = withContext(Dispatchers.Default) {
                reader.detect(px, w, h)
            } ?: return@launch
            val side = withContext(Dispatchers.Default) {
                reader.updateSide(detection.layout)
            }
            val fen = ScreenBoardReader.fen(detection.layout, side)

            if (AssistantState.boardRect.value == null) {
                val grid = detection.grid
                AssistantState.setBoardRect(Rect(grid.left, grid.top, grid.right, grid.bottom))
                AssistantState.setMessage("Board terdeteksi otomatis dari layar")
            }
            publish(fen, emptyList(), source, "Board terdeteksi dari layar (screenshot)")
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
        try {
            val path = Path().apply { moveTo(p.x, p.y) }
            val stroke = GestureDescription.StrokeDescription(path, 0, 70)
            val gesture = GestureDescription.Builder().addStroke(stroke).build()
            dispatchGesture(gesture, null, null)
        } catch (e: Exception) {
            // Ignore gesture dispatch errors
        }
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
        private const val SCREEN_CAPTURE_INTERVAL_MS = 600L

        @Volatile
        private var instance: ChessAccessibilityService? = null

        /** Called from the overlay panel to force the AI move right now. */
        @JvmStatic
        fun playBestMoveNow() {
            instance?.playBestMove(force = true)
        }
    }
}
