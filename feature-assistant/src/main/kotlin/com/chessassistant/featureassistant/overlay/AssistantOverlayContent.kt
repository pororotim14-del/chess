package com.chessassistant.featureassistant.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chessassistant.corechess.model.Color as ChessColor
import com.chessassistant.corechess.notation.FenParser
import com.chessassistant.featureassistant.assistant.AssistantState
import com.chessassistant.coreui.theme.ChessAssistantTheme
import com.chessassistant.coreui.theme.BoardDark
import com.chessassistant.coreui.theme.BoardLight
import com.chessassistant.coreui.theme.CheckRed
import com.chessassistant.coreui.theme.ChessGreenDark
import com.chessassistant.coreui.theme.PastelGreen
import kotlin.math.exp

@Composable
fun AssistantOverlayContent(
    expanded: Boolean,
    onToggleExpand: (Boolean) -> Unit,
    onClose: () -> Unit,
    onFocusableChange: (Boolean) -> Unit,
    onCalibrate: () -> Unit,
    onPlayNow: () -> Unit,
    onToggleAutoPlay: (Boolean) -> Unit,
    onSetEngineColor: (ChessColor) -> Unit,
    onSetFlipped: (Boolean) -> Unit,
    onLoadFen: (String) -> Unit,
) {
    ChessAssistantTheme {
        val running by AssistantState.running.collectAsState()
        val fen by AssistantState.fen.collectAsState()
        val source by AssistantState.sourceApp.collectAsState()
        val hint by AssistantState.detectionHint.collectAsState()
        val analysis by AssistantState.analysis.collectAsState()
        val autoPlay by AssistantState.autoPlay.collectAsState()
        val engineColor by AssistantState.engineColor.collectAsState()
        val flipped by AssistantState.boardFlipped.collectAsState()
        val rect by AssistantState.boardRect.collectAsState()
        val message by AssistantState.message.collectAsState()
        val moves by AssistantState.moves.collectAsState()

        val bg = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
        val sideToMove = fen?.let { FenParser.parse(it)?.sideToMove }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(bg)
                .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HeaderRow(
                running = running,
                expanded = expanded,
                bestMove = analysis.bestMove,
                onToggleExpand = { onToggleExpand(!expanded) },
                onClose = onClose,
            )

            if (expanded) {
                EvalBar(
                    analysisEval = analysis.evalCp,
                    sideToMove = sideToMove,
                    bestMove = analysis.bestMove,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Best move",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = formatEval(analysis.evalCp, sideToMove),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                if (analysis.pv.isNotBlank()) {
                    Text(
                        text = analysis.pv,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                    )
                }

                InfoRow("App", source ?: "-")
                InfoRow("Deteksi", hint)
                fen?.let { InfoRow("FEN", it.take(42) + if (it.length > 42) "..." else "") }
                InfoRow("Langkah", "${moves.size}")

                message?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("AI auto-play", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.weight(1f))
                    Switch(checked = autoPlay, onCheckedChange = onToggleAutoPlay)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("AI main sebagai", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.weight(1f))
                    FilterChip(
                        selected = engineColor == ChessColor.WHITE,
                        onClick = { onSetEngineColor(ChessColor.WHITE) },
                        label = { Text("Putih") },
                    )
                    FilterChip(
                        selected = engineColor == ChessColor.BLACK,
                        onClick = { onSetEngineColor(ChessColor.BLACK) },
                        label = { Text("Hitam") },
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Board dibalik", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.weight(1f))
                    Switch(checked = flipped, onCheckedChange = onSetFlipped)
                }

                if (rect == null) {
                    Text(
                        text = "Papan belum dikalibrasi — auto-play butuh posisi board.",
                        style = MaterialTheme.typography.bodySmall,
                        color = CheckRed,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(onClick = onPlayNow, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.PlayArrow, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Jalankan AI")
                    }
                    OutlinedButton(onClick = onCalibrate, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Gesture, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Kalibrasi")
                    }
                }

                FenInput(onFocusableChange = onFocusableChange, onLoadFen = onLoadFen)
            }
        }
    }
}

@Composable
private fun HeaderRow(
    running: Boolean,
    expanded: Boolean,
    bestMove: String,
    onToggleExpand: () -> Unit,
    onClose: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggleExpand),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(if (running) PastelGreen else MaterialTheme.colorScheme.outline),
        )
        Spacer(Modifier.width(8.dp))
        Icon(Icons.Filled.SmartToy, null, modifier = Modifier.size(18.dp), tint = ChessGreenDark)
        Spacer(Modifier.width(6.dp))
        Text("Engine Asisten", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        if (!expanded && bestMove.isNotEmpty()) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = bestMove,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.weight(1f))
        Icon(
            if (expanded) Icons.Filled.ExpandMore else Icons.Filled.ExpandLess,
            null,
            modifier = Modifier.size(18.dp),
        )
        IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Filled.Close, "Tutup", modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun EvalBar(
    analysisEval: Int,
    sideToMove: ChessColor?,
    bestMove: String,
) {
    val whiteFraction = 1.0f / (1.0f + exp(-analysisEval / 400.0f))
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        if (bestMove.isEmpty()) {
            Text(
                text = "Checkmate / Stalemate",
                style = MaterialTheme.typography.labelMedium,
                color = CheckRed,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(Brush.horizontalGradient(listOf(BoardLight, BoardDark))),
        ) {
            val whiteWidth = (whiteFraction * 100)
            Box(
                modifier = Modifier
                    .fillMaxWidth(whiteWidth)
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(Color.White),
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(64.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun FenInput(
    onFocusableChange: (Boolean) -> Unit,
    onLoadFen: (String) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("Tempel FEN (manual)") },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { onFocusableChange(it.isFocused) },
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            OutlinedButton(onClick = {
                onLoadFen(text.trim())
                text = ""
                onFocusableChange(false)
            }) {
                Text("Muat posisi")
            }
        }
    }
}

internal fun formatEval(cp: Int, sideToMove: ChessColor?): String {
    val adjusted = if (sideToMove == ChessColor.BLACK) -cp else cp
    if (adjusted >= 100000) return "Mate"
    if (adjusted <= -100000) return "-Mate"
    val value = adjusted / 100.0
    return if (value >= 0) String.format("+%.2f", value) else String.format("%.2f", value)
}
