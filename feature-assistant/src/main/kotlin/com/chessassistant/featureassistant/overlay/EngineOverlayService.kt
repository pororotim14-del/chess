package com.chessassistant.featureassistant.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.chessassistant.corechess.model.Color
import com.chessassistant.featureassistant.assistant.AssistantPrefs
import com.chessassistant.featureassistant.assistant.AssistantState
import com.chessassistant.security.engine.EngineSecurityManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import javax.inject.Inject

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
    private var hasOverlayPermission = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        prefs = AssistantPrefs(applicationContext)
        hasOverlayPermission = checkOverlayPermission()
        
        if (!hasOverlayPermission) {
            AssistantState.setMessage("Izin overlay tidak diberikan. Buka pengaturan untuk mengaktifkan.")
            stopSelf()
            return
        }
        
        // Create security infrastructure directly
        val securityManager = com.chessassistant.security.SecurityManager(
            com.chessassistant.security.DeviceFingerprint(
                model = android.os.Build.MODEL,
                board = android.os.Build.BOARD,
                manufacturer = android.os.Build.MANUFACTURER,
                bootId = null,
            ),
            com.chessassistant.security.AndroidKeyStoreSecretStorage()
        )
        val engineSecurityManager = com.chessassistant.security.engine.EngineSecurityManager.getInstance(applicationContext, securityManager)
        
        AssistantState.setBoardRect(prefs.loadBoardRect())
        AssistantState.setBoardFlipped(prefs.loadFlipped())
        AssistantState.setAutoPlay(prefs.loadAutoPlay())
        AssistantState.setEngineColor(prefs.loadEngineColor())
        AssistantState.setRunning(true)
        AssistantAnalyzer.launch(scope, engineSecurityManager)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!hasOverlayPermission) {
            stopSelf()
            return START_NOT_STICKY
        }
        
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

    private fun checkOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
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
        try {
            windowManager.addView(content, panelParams())
        } catch (e: Exception) {
            AssistantState.setMessage("Gagal menampilkan overlay: ${e.message}")
            removePanel()
        }
    }

    private fun removePanel() {
        panelView?.let { windowManager.removeView(it) }
        panelView = null
    }

    private fun updatePanelWindow() {
        val view = panelView ?: return
        try {
            windowManager.updateViewLayout(view, panelParams())
        } catch (e: Exception) {
            // Ignore layout update errors
        }
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
        try {
            windowManager.addView(content, calibrationParams())
        } catch (e: Exception) {
            AssistantState.setMessage("Gagal menampilkan kalibrasi: ${e.message}")
            removeCalibration()
        }
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
        val channelId = "trx_chess_overlay"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(channelId, "TRX-CHESS Engine", NotificationManager.IMPORTANCE_LOW)
            manager.createNotificationChannel(channel)
        }
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            packageManager.getLaunchIntentForPackage(packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("TRX-CHESS Engine aktif")
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
        const val ACTION_START = "com.trxchess.overlay.START"
        const val ACTION_STOP = "com.trxchess.overlay.STOP"
        const val ACTION_CALIBRATE = "com.trxchess.overlay.CALIBRATE"

        fun start(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                // Permission not granted, cannot start
                return
            }
            val i = Intent(context, EngineOverlayService::class.java).setAction(ACTION_START)
            ContextCompat.startForegroundService(context, i)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, EngineOverlayService::class.java))
        }

        fun startCalibrate(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                return
            }
            val i = Intent(context, EngineOverlayService::class.java).setAction(ACTION_CALIBRATE)
            ContextCompat.startForegroundService(context, i)
        }
    }
}
