package com.chessassistant.featureassistant.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import androidx.compose.ui.Alignment
import com.chessassistant.coreui.theme.CheckRed
import com.chessassistant.coreui.theme.PastelGreen
import kotlin.math.roundToInt

/**
 * Full-screen calibration window: the user drags the four corner handles onto
 * the corners of the board shown by the underlying chess app, then saves.
 * Corners are stored as fractions so the layout scales to any screen.
 */
@Composable
fun CalibrationOverlayContent(
    initial: android.graphics.Rect?,
    screenSize: Pair<Int, Int>,
    onSave: (android.graphics.Rect) -> Unit,
    onCancel: () -> Unit,
) {
    val (screenW, screenH) = screenSize

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val containerW = maxWidth
        val containerH = maxHeight

        val corners = remember(initial, containerW, containerH) {
            if (initial != null && screenW > 0 && screenH > 0) {
                val cw = containerW.value
                val ch = containerH.value
                Quad(
                    tl = Offset(initial.left.toFloat() / screenW * cw, initial.top.toFloat() / screenH * ch),
                    tr = Offset(initial.right.toFloat() / screenW * cw, initial.top.toFloat() / screenH * ch),
                    br = Offset(initial.right.toFloat() / screenW * cw, initial.bottom.toFloat() / screenH * ch),
                    bl = Offset(initial.left.toFloat() / screenW * cw, initial.bottom.toFloat() / screenH * ch),
                )
            } else {
                Quad(
                    tl = Offset(0.1f * containerW.value, 0.1f * containerH.value),
                    tr = Offset(0.9f * containerW.value, 0.1f * containerH.value),
                    br = Offset(0.9f * containerW.value, 0.9f * containerH.value),
                    bl = Offset(0.1f * containerW.value, 0.9f * containerH.value),
                )
            }
        }

        var topLeft by remember { mutableStateOf(corners.tl) }
        var topRight by remember { mutableStateOf(corners.tr) }
        var bottomRight by remember { mutableStateOf(corners.br) }
        var bottomLeft by remember { mutableStateOf(corners.bl) }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.25f)),
        ) {
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(topLeft.x * size.width / containerW.value, topLeft.y * size.height / containerH.value)
                lineTo(topRight.x * size.width / containerW.value, topRight.y * size.height / containerH.value)
                lineTo(bottomRight.x * size.width / containerW.value, bottomRight.y * size.height / containerH.value)
                lineTo(bottomLeft.x * size.width / containerW.value, bottomLeft.y * size.height / containerH.value)
                close()
            }
            drawPath(path, color = PastelGreen, style = Stroke(width = 4f))
        }

        CornerHandle(topLeft, containerW, containerH, CheckRed) { topLeft = it }
        CornerHandle(topRight, containerW, containerH, Color(0xFFFFB74D)) { topRight = it }
        CornerHandle(bottomRight, containerW, containerH, PastelGreen) { bottomRight = it }
        CornerHandle(bottomLeft, containerW, containerH, Color(0xFF64B5F6)) { bottomLeft = it }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(12.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Text(
                text = "Tarik 4 titik ke sudut papan catur, lalu tekan Simpan",
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(onClick = onCancel) { Text("Batal") }
            Button(onClick = {
                val rect = toRect(
                    topLeft, topRight, bottomRight, bottomLeft,
                    containerW, containerH, screenW, screenH,
                )
                onSave(rect)
            }) { Text("Simpan") }
        }
    }
}

@Composable
private fun CornerHandle(
    pos: Offset,
    containerW: Dp,
    containerH: Dp,
    color: Color,
    onDrag: (Offset) -> Unit,
) {
    val density = LocalDensity.current
    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    pos.x.roundToInt(),
                    pos.y.roundToInt(),
                )
            }
            .size(30.dp)
            .clip(CircleShape)
            .background(color)
            .border(2.dp, Color.White, CircleShape)
            .pointerInput(Unit) {
                detectDragGestures { change, drag ->
                    change.consume()
                    val dx = with(density) { drag.x.toDp() }.value
                    val dy = with(density) { drag.y.toDp() }.value
                    onDrag(
                        Offset(
                            (pos.x + dx).coerceIn(0f, containerW.value),
                            (pos.y + dy).coerceIn(0f, containerH.value),
                        ),
                    )
                }
            },
    )
}

private data class Quad(
    val tl: Offset,
    val tr: Offset,
    val br: Offset,
    val bl: Offset,
)

private fun toRect(
    tl: Offset,
    tr: Offset,
    br: Offset,
    bl: Offset,
    containerW: Dp,
    containerH: Dp,
    screenW: Int,
    screenH: Int,
): android.graphics.Rect {
    val cw = containerW.value
    val ch = containerH.value
    val xs = listOf(tl.x, tr.x, br.x, bl.x).map { it / cw * screenW }
    val ys = listOf(tl.y, tr.y, br.y, bl.y).map { it / ch * screenH }
    val left = (xs.minOrNull() ?: 0f).toInt()
    val top = (ys.minOrNull() ?: 0f).toInt()
    val right = (xs.maxOrNull() ?: 0f).toInt()
    val bottom = (ys.maxOrNull() ?: 0f).toInt()
    return android.graphics.Rect(left, top, right, bottom)
}
