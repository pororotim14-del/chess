package com.chessassistant.featureassistant

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chessassistant.corechess.model.Color
import com.chessassistant.featureassistant.a11y.ChessAccessibilityService
import com.chessassistant.featureassistant.assistant.AssistantPrefs
import com.chessassistant.featureassistant.assistant.AssistantState
import com.chessassistant.featureassistant.overlay.EngineOverlayService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AssistantViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val prefs = AssistantPrefs(context)

    val running = AssistantState.running
    val fen = AssistantState.fen
    val sourceApp = AssistantState.sourceApp
    val detectionHint = AssistantState.detectionHint
    val analysis = AssistantState.analysis
    val autoPlay = AssistantState.autoPlay
    val engineColor = AssistantState.engineColor
    val boardFlipped = AssistantState.boardFlipped
    val boardRect = AssistantState.boardRect
    val moves = AssistantState.moves
    val message = AssistantState.message

    fun start() {
        EngineOverlayService.start(context)
    }

    fun stop() {
        EngineOverlayService.stop(context)
    }

    fun calibrate() {
        if (!AssistantState.running.value) start()
        EngineOverlayService.startCalibrate(context)
    }

    fun hasOverlayPermission(): Boolean = Settings.canDrawOverlays(context)

    fun accessibilityEnabled(): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        val expected = ComponentName(context, ChessAccessibilityService::class.java).flattenToString()
        return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
    }

    fun openAccessibilitySettings() {
        runCatching { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
    }

    fun openOverlaySettings() {
        runCatching {
            context.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}"),
                ),
            )
        }
    }

    fun toggleAutoPlay(value: Boolean) {
        AssistantState.setAutoPlay(value)
        prefs.saveAutoPlay(value)
    }

    fun setEngineColor(color: Color) {
        AssistantState.setEngineColor(color)
        prefs.saveEngineColor(color)
    }

    fun setFlipped(value: Boolean) {
        AssistantState.setBoardFlipped(value)
        prefs.saveFlipped(value)
    }

    fun loadFen(fen: String) {
        viewModelScope.launch {
            AssistantState.setPosition(fen.trim(), emptyList(), "Manual", "FEN dimasukkan manual")
        }
    }
}
