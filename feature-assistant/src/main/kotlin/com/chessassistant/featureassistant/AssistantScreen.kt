package com.chessassistant.featureassistant

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.chessassistant.corechess.model.Color
import com.chessassistant.featureassistant.overlay.formatEval
import com.chessassistant.featureassistant.assistant.AssistantState
import com.chessassistant.coreui.theme.PastelGreen

@Composable
fun AssistantScreen(
    viewModel: AssistantViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val running by viewModel.running.collectAsState()
    val fen by viewModel.fen.collectAsState()
    val source by viewModel.sourceApp.collectAsState()
    val hint by viewModel.detectionHint.collectAsState()
    val analysis by viewModel.analysis.collectAsState()
    val autoPlay by viewModel.autoPlay.collectAsState()
    val engineColor by viewModel.engineColor.collectAsState()
    val flipped by viewModel.boardFlipped.collectAsState()
    val rect by viewModel.boardRect.collectAsState()
    val moves by viewModel.moves.collectAsState()
    val message by viewModel.message.collectAsState()

    val hasOverlay = viewModel.hasOverlayPermission()
    val a11yOn = viewModel.accessibilityEnabled()

    var fenText by remember { mutableStateOf("") }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SectionHeader()

            StatusCard(running = running)

            Button(
                onClick = {
                    if (running) viewModel.stop()
                    else {
                        if (android.os.Build.VERSION.SDK_INT >= 33 &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                            != PackageManager.PERMISSION_GRANTED
                        ) {
                            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        viewModel.start()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Icon(
                    if (running) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                    null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (running) "Stop Engine" else "Mulai Engine (Live)",
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            PermissionCard(
                hasOverlay = hasOverlay,
                a11yOn = a11yOn,
                onOpenOverlay = viewModel::openOverlaySettings,
                onOpenA11y = viewModel::openAccessibilitySettings,
            )

            DetectionCard(
                running = running,
                source = source,
                hint = hint,
                fen = fen,
                bestMove = analysis.bestMove,
                eval = analysis.evalCp,
                pv = analysis.pv,
                moves = moves.size,
                message = message,
                calibrated = rect != null,
            )

            AICard(
                autoPlay = autoPlay,
                engineColor = engineColor,
                flipped = flipped,
                onAutoPlay = viewModel::toggleAutoPlay,
                onEngineColor = viewModel::setEngineColor,
                onFlipped = viewModel::setFlipped,
            )

            OutlinedButton(
                onClick = viewModel::calibrate,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Gesture, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Kalibrasi Posisi Board (untuk AI auto-play)")
            }

            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Posisi Manual (fallback)", style = MaterialTheme.typography.titleSmall)
                    OutlinedTextField(
                        value = fenText,
                        onValueChange = { fenText = it },
                        label = { Text("FEN") },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
                        onClick = {
                            viewModel.loadFen(fenText)
                            fenText = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Muat Posisi")
                    }
                }
            }

            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Cara pakai", style = MaterialTheme.typography.titleSmall)
                    HowToStep("1", "Nyalakan engine dengan tombol Mulai.")
                    HowToStep("2", "Izinkan overlay + layanan aksesibilitas (satu kali).")
                    HowToStep("3", "Buka aplikasi catur apa pun — board terbaca otomatis dari layar (screenshot).")
                    HowToStep("4", "Hidupkan AI auto-play agar AI mengetuk langkahnya sendiri di board.")
                    HowToStep("5", "Kalibrasi posisi board hanya jika deteksi otomatis kurang pas.")
                }
            }
        }
    }
}

@Composable
private fun SectionHeader() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.SmartToy,
                null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(26.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text("Engine Asisten", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "Analisis real-time di latar belakang untuk semua app catur",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StatusCard(running: Boolean) {
    Card {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(if (running) PastelGreen else MaterialTheme.colorScheme.outline),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                if (running) "Engine berjalan di latar belakang" else "Engine dalam keadaan berhenti",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun PermissionCard(
    hasOverlay: Boolean,
    a11yOn: Boolean,
    onOpenOverlay: () -> Unit,
    onOpenA11y: () -> Unit,
) {
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Izin yang dibutuhkan", style = MaterialTheme.typography.titleSmall)
            PermissionRow(
                "Overlay (tampilan asisten di atas app lain)",
                granted = hasOverlay,
                actionLabel = "Izinkan",
                onAction = onOpenOverlay,
            )
            PermissionRow(
                "Layanan aksesibilitas (membaca board dari app catur lain)",
                granted = a11yOn,
                actionLabel = "Buka Pengaturan",
                onAction = onOpenA11y,
            )
        }
    }
}

@Composable
private fun PermissionRow(
    label: String,
    granted: Boolean,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (granted) Icons.Filled.Check else Icons.Filled.Settings,
            null,
            tint = if (granted) PastelGreen else MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
        )
        if (!granted) {
            OutlinedButton(onClick = onAction, modifier = Modifier.height(34.dp)) {
                Text(actionLabel, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun DetectionCard(
    running: Boolean,
    source: String?,
    hint: String,
    fen: String?,
    bestMove: String,
    eval: Int,
    pv: String,
    moves: Int,
    message: String?,
    calibrated: Boolean,
) {
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Status deteksi", style = MaterialTheme.typography.titleSmall)
            DetectionRow("Aplikasi", source ?: "-")
            DetectionRow("Deteksi", hint)
            DetectionRow("FEN", fen ?: "-")
            DetectionRow("Best move", bestMove.ifEmpty { "-" })
            DetectionRow("Evaluasi", formatEval(eval, null))
            DetectionRow("Langkah", "$moves")
            DetectionRow("Kalibrasi", if (calibrated) "Siap" else "Belum")
            if (pv.isNotBlank()) {
                Text(
                    pv,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                )
            }
            message?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
            }
            if (!running) {
                Text(
                    "Engine berhenti — nyalakan untuk mulai menganalisis.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DetectionRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(88.dp),
        )
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun AICard(
    autoPlay: Boolean,
    engineColor: Color,
    flipped: Boolean,
    onAutoPlay: (Boolean) -> Unit,
    onEngineColor: (Color) -> Unit,
    onFlipped: (Boolean) -> Unit,
) {
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Pengaturan AI", style = MaterialTheme.typography.titleSmall)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("AI auto-play (mengetuk langkah di app lain)", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.weight(1f))
                Switch(checked = autoPlay, onCheckedChange = onAutoPlay)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("AI main sebagai", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.weight(1f))
                FilterChip(
                    selected = engineColor == Color.WHITE,
                    onClick = { onEngineColor(Color.WHITE) },
                    label = { Text("Putih") },
                )
                Spacer(Modifier.width(6.dp))
                FilterChip(
                    selected = engineColor == Color.BLACK,
                    onClick = { onEngineColor(Color.BLACK) },
                    label = { Text("Hitam") },
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Board dibalik", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.weight(1f))
                Switch(checked = flipped, onCheckedChange = onFlipped)
            }
        }
    }
}

@Composable
private fun HowToStep(number: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Text(number, color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelSmall)
        }
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}
