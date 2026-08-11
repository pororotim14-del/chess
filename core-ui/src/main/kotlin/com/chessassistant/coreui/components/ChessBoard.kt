package com.chessassistant.coreui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chessassistant.corechess.model.Piece
import com.chessassistant.corechess.model.Color as PieceColor
import com.chessassistant.corechess.model.Square
import com.chessassistant.coreui.theme.BoardDark
import com.chessassistant.coreui.theme.BoardLight
import com.chessassistant.coreui.theme.CheckRed
import com.chessassistant.coreui.theme.LastMove
import com.chessassistant.coreui.theme.LegalDot
import com.chessassistant.coreui.theme.Selected

/**
 * Chess board grid. Square index follows the engine's convention where
 * index = rank * 8 + file and rank 1 sits at the bottom (unless flipped).
 */
@Composable
fun ChessBoard(
    board: Array<Piece?>,
    selected: Int? = null,
    legalTargets: Set<Int> = emptySet(),
    lastMove: Pair<Int, Int>? = null,
    kingInCheck: Int? = null,
    flipped: Boolean = false,
    onSquareClick: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .aspectRatio(1f)
            .border(1.dp, Color.Black),
    ) {
        val cellSize = maxWidth / 8
        val fontSize = with(LocalDensity.current) { (cellSize * 0.62f).value.sp }
        val rows = if (flipped) 0..7 else 7 downTo 0
        val cols = if (flipped) 7 downTo 0 else 0..7

        Column(Modifier.fillMaxSize()) {
            for (displayRow in rows) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    for (displayCol in cols) {
                        val file = if (flipped) 7 - displayCol else displayCol
                        val rank = if (flipped) 7 - displayRow else displayRow
                        val square = rank * 8 + file
                        val dark = (file + rank) % 2 == 1
                        Cell(
                            square = square,
                            piece = board[square],
                            dark = dark,
                            selected = square == selected,
                            highlighted = square == kingInCheck,
                            lastMove = square == lastMove?.first || square == lastMove?.second,
                            hasLegalTarget = square in legalTargets,
                            fontSize = fontSize,
                            onClick = { onSquareClick(square) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.Cell(
    square: Int,
    piece: Piece?,
    dark: Boolean,
    selected: Boolean,
    highlighted: Boolean,
    lastMove: Boolean,
    hasLegalTarget: Boolean,
    fontSize: androidx.compose.ui.unit.TextUnit,
    onClick: () -> Unit,
) {
    val base = if (dark) BoardDark else BoardLight
    val bg = when {
        highlighted -> CheckRed
        selected -> Selected
        lastMove -> LastMove
        else -> base
    }
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .background(bg)
            .drawBehind {
                if (hasLegalTarget) {
                    drawCircle(
                        color = LegalDot,
                        radius = size.minDimension * 0.16f,
                        center = center,
                    )
                }
            }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (piece != null) {
            Text(
                text = piece.glyph(),
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                color = if (piece.color == PieceColor.WHITE) Color.White else Color(0xFF101010),
            )
        }
    }
}

private fun Piece.glyph(): String = when (type.ordinal) {
    0 -> if (color == PieceColor.WHITE) "\u2659" else "\u265F" // pawn
    1 -> if (color == PieceColor.WHITE) "\u2658" else "\u265E" // knight
    2 -> if (color == PieceColor.WHITE) "\u2657" else "\u265D" // bishop
    3 -> if (color == PieceColor.WHITE) "\u2656" else "\u265C" // rook
    4 -> if (color == PieceColor.WHITE) "\u2655" else "\u265B" // queen
    5 -> if (color == PieceColor.WHITE) "\u2654" else "\u265A" // king
    else -> "?"
}