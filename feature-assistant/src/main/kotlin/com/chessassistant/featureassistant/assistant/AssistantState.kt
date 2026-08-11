package com.chessassistant.featureassistant.assistant

import android.graphics.Rect
import com.chessassistant.corechess.model.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Process-wide shared state between the accessibility reader, the overlay
 * service and the in-app assistant screen. All three live in the same
 * process, so a singleton object is enough to keep them in sync.
 */
object AssistantState {

    data class Analysis(
        val bestMove: String = "",
        val evalCp: Int = 0,
        val pv: String = "",
        val updatedAt: Long = 0L,
    )

    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running.asStateFlow()

    private val _fen = MutableStateFlow<String?>(null)
    val fen: StateFlow<String?> = _fen.asStateFlow()

    private val _moves = MutableStateFlow<List<String>>(emptyList())
    val moves: StateFlow<List<String>> = _moves.asStateFlow()

    private val _sourceApp = MutableStateFlow<String?>(null)
    val sourceApp: StateFlow<String?> = _sourceApp.asStateFlow()

    private val _detectionHint = MutableStateFlow("Menunggu deteksi posisi...")
    val detectionHint: StateFlow<String> = _detectionHint.asStateFlow()

    private val _analysis = MutableStateFlow(Analysis())
    val analysis: StateFlow<Analysis> = _analysis.asStateFlow()

    private val _autoPlay = MutableStateFlow(false)
    val autoPlay: StateFlow<Boolean> = _autoPlay.asStateFlow()

    private val _engineColor = MutableStateFlow(Color.BLACK)
    val engineColor: StateFlow<Color> = _engineColor.asStateFlow()

    private val _boardFlipped = MutableStateFlow(false)
    val boardFlipped: StateFlow<Boolean> = _boardFlipped.asStateFlow()

    private val _boardRect = MutableStateFlow<Rect?>(null)
    val boardRect: StateFlow<Rect?> = _boardRect.asStateFlow()

    private val _lastTapFen = MutableStateFlow<String?>(null)
    val lastTapFen: StateFlow<String?> = _lastTapFen.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun setRunning(value: Boolean) {
        _running.value = value
    }

    fun setPosition(fen: String?, moves: List<String>, source: String?, hint: String) {
        _fen.value = fen
        _moves.value = moves
        if (source != null) _sourceApp.value = source
        _detectionHint.value = hint
    }

    fun setAnalysis(analysis: Analysis) {
        _analysis.value = analysis
    }

    fun setAutoPlay(value: Boolean) {
        _autoPlay.value = value
    }

    fun setEngineColor(color: Color) {
        _engineColor.value = color
    }

    fun setBoardFlipped(value: Boolean) {
        _boardFlipped.value = value
    }

    fun setBoardRect(rect: Rect?) {
        _boardRect.value = rect
    }

    fun markAutoTap(fen: String) {
        _lastTapFen.value = fen
    }

    fun setMessage(message: String?) {
        _message.value = message
    }
}
