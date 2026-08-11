package com.chessassistant.featureassistant.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import com.chessassistant.corechess.model.Color
import com.chessassistant.featureassistant.assistant.AssistantPrefs
import com.chessassistant.featureassistant.assistant.AssistantState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * Foreground service that hosts the floating engine-assistant panel over any
 * app, plus an optional full-screen calibration window for the AI auto-play
 * tap target.
 */
class EngineOverlayService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var windowManager: WindowManager
    private lateinit var prefs: AssistantPrefs

    private var panelView: View? = null
    private var calibrationView: View? = null
    private var panelExpanded = true
    private var wantsFocus = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        prefs = AssistantPrefs(applicationContext)
        AssistantState.setBoardRect(prefs.loadBoardRect())
        AssistantState.setBoardFlipped(prefs.loadFlipped())
        AssistantState.setAutoPlay(prefs.loadAutoPlay())
        AssistantState.setEngineColor(prefs.loadEngineColor())
        AssistantState.setRunning(true)
        AssistantAnalyzer.launch(scope)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundCompat()
        if (intent?.action == ACTION_CALIBRATE) {
            showCalibration()
        } else {
            showPanel()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        AssistantState.setRunning(false)
        AssistantState.setBoardRect(null)
        scope.cancel()
        removePanel()
        removeCalibration()
        super.onDestroy()
    }

    // ------------------------------------------------------------------ panel

    private fun showPanel() {
        if (panelView != null) return
        val content = ComposeView(this).apply {
            setContent {
                AssistantOverlayContent(
                    expanded = panelExpanded,
                    onToggleExpand = { panelExpanded = it; updatePanelWindow() },
                    onClose = { removePanel() },
                    onFocusableChange = { wantsFocus = it; updatePanelWindow() },
                    onCalibrate = { showCalibration() },
                    onPlayNow = { com.chessassistant.featureassistant.a11y.ChessAccessibilityService.playBestMoveNow() },
                    onToggleAutoPlay = { v ->
                        AssistantState.setAutoPlay(v)
                        prefs.saveAutoPlay(v)
                    },
                    onSetEngineColor = { c ->
                        AssistantState.setEngineColor(c)
                        prefs.saveEngineColor(c)
                    },
                    onSetFlipped = { v ->
                        AssistantState.setBoardFlipped(v)
                        prefs.saveFlipped(v)
                    },
                    onLoadFen = { fen ->
                        AssistantState.setPosition(fen, emptyList(), "Manual", "FEN dimasukkan manual")
                    },
                )
            }
        }
        panelView = content
        windowManager.addView(content, panelParams())
    }

    private fun removePanel() {
        panelView?.let { windowManager.removeView(it) }
        panelView = null
    }

    private fun updatePanelWindow() {
        val view = panelView ?: return
        runCatching { windowManager.updateViewLayout(view, panelParams()) }
    }

    private fun panelParams(): WindowManager.LayoutParams {
        val wm = windowManager
        val dm = DisplayMetrics()
        val screenW = if (Build.VERSION.SDK_INT >= 30) {
            wm.currentWindowMetrics.bounds.width()
        } else {
            @Suppress("DEPRECATION") wm.defaultDisplay.getMetrics(dm); dm.widthPixels
        }
        val width = if (panelExpanded) (screenW * 0.92f).toInt() else dp(140)
        return WindowManager.LayoutParams(
            width,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            windowFlags(),
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            y = dp(8)
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }
    }

    private fun windowFlags(): Int {
        var flags = WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        if (!wantsFocus) flags = flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        return flags
    }

    // ----------------------------------------------------------- calibration

    private fun showCalibration() {
        if (calibrationView != null) return
        removePanel()
        val content = ComposeView(this).apply {
            setContent {
                CalibrationOverlayContent(
                    initial = AssistantState.boardRect.value,
                    screenSize = screenSize(),
                    onSave = { rect ->
                        AssistantState.setBoardRect(rect)
                        prefs.saveBoardRect(rect)
                        AssistantState.setMessage("Posisi board tersimpan")
                        finishCalibration()
                    },
                    onCancel = { finishCalibration() },
                )
            }
        }
        calibrationView = content
        windowManager.addView(content, calibrationParams())
    }

    private fun removeCalibration() {
        calibrationView?.let { windowManager.removeView(it) }
        calibrationView = null
    }

    private fun finishCalibration() {
        removeCalibration()
        showPanel()
    }

    private fun calibrationParams(): WindowManager.LayoutParams =
        WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

    private fun screenSize(): Pair<Int, Int> {
        val wm = windowManager
        val bounds = if (Build.VERSION.SDK_INT >= 30) {
            wm.currentWindowMetrics.bounds
        } else {
            val dm = DisplayMetrics()
            @Suppress("DEPRECATION") wm.defaultDisplay.getMetrics(dm)
            Rect(0, 0, dm.widthPixels, dm.heightPixels)
        }
        return bounds.width() to bounds.height()
    }

    // ------------------------------------------------------------ foreground

    private fun startForegroundCompat() {
        val channelId = "chess_assistant_overlay"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(channelId, "Engine Asisten", NotificationManager.IMPORTANCE_LOW)
            manager.createNotificationChannel(channel)
        }
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            packageManager.getLaunchIntentForPackage(packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Engine Asisten aktif")
            .setContentText("Menganalisis posisi catur secara real-time")
            .setContentIntent(contentIntent)
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setOngoing(true)
            .build()
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val NOTIFICATION_ID = 101
        const val ACTION_START = "com.chessassistant.overlay.START"
        const val ACTION_STOP = "com.chessassistant.overlay.STOP"
        const val ACTION_CALIBRATE = "com.chessassistant.overlay.CALIBRATE"

        fun start(context: Context) {
            val i = Intent(context, EngineOverlayService::class.java).setAction(ACTION_START)
            context.startForegroundService(i)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, EngineOverlayService::class.java))
        }

        fun startCalibrate(context: Context) {
            val i = Intent(context, EngineOverlayService::class.java).setAction(ACTION_CALIBRATE)
            context.startForegroundService(i)
        }
    }
}
