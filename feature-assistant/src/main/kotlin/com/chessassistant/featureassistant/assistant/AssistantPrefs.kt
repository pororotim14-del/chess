package com.chessassistant.featureassistant.assistant

import android.content.Context
import android.graphics.Rect
import com.chessassistant.corechess.model.Color

/** Tiny persistence for assistant settings (board calibration, AI mode). */
class AssistantPrefs(context: Context) {

    private val prefs = context.getSharedPreferences("assistant_prefs", Context.MODE_PRIVATE)

    fun loadBoardRect(): Rect? {
        val l = prefs.getInt(KEY_LEFT, -1)
        val t = prefs.getInt(KEY_TOP, -1)
        val r = prefs.getInt(KEY_RIGHT, -1)
        val b = prefs.getInt(KEY_BOTTOM, -1)
        if (l < 0 || t < 0 || r <= l || b <= t) return null
        return Rect(l, t, r, b)
    }

    fun saveBoardRect(rect: Rect) {
        prefs.edit()
            .putInt(KEY_LEFT, rect.left)
            .putInt(KEY_TOP, rect.top)
            .putInt(KEY_RIGHT, rect.right)
            .putInt(KEY_BOTTOM, rect.bottom)
            .apply()
    }

    fun clearBoardRect() {
        prefs.edit().remove(KEY_LEFT).remove(KEY_TOP).remove(KEY_RIGHT).remove(KEY_BOTTOM).apply()
    }

    fun loadFlipped(): Boolean = prefs.getBoolean(KEY_FLIPPED, false)

    fun saveFlipped(value: Boolean) = prefs.edit().putBoolean(KEY_FLIPPED, value).apply()

    fun loadAutoPlay(): Boolean = prefs.getBoolean(KEY_AUTOPLAY, false)

    fun saveAutoPlay(value: Boolean) = prefs.edit().putBoolean(KEY_AUTOPLAY, value).apply()

    fun loadEngineColor(): Color =
        if (prefs.getBoolean(KEY_ENGINE_WHITE, false)) Color.WHITE else Color.BLACK

    fun saveEngineColor(color: Color) =
        prefs.edit().putBoolean(KEY_ENGINE_WHITE, color == Color.WHITE).apply()

    companion object {
        private const val KEY_LEFT = "board_left"
        private const val KEY_TOP = "board_top"
        private const val KEY_RIGHT = "board_right"
        private const val KEY_BOTTOM = "board_bottom"
        private const val KEY_FLIPPED = "board_flipped"
        private const val KEY_AUTOPLAY = "auto_play"
        private const val KEY_ENGINE_WHITE = "engine_white"
    }
}
