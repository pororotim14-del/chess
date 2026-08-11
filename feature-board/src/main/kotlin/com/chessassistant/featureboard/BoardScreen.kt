package com.chessassistant.featureboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chessassistant.coreui.components.ChessBoard

@Composable
fun BoardScreen(
    state: BoardUiState,
    onSquareClick: (Int) -> Unit,
    onNewGame: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onFlip: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (state.pieceSnapshot.isNotEmpty()) {
                ChessBoard(
                    board = state.pieceSnapshot.toTypedArray(),
                    selected = state.selected,
                    legalTargets = state.legalTargets,
                    lastMove = state.lastMove,
                    kingInCheck = state.kingInCheck,
                    flipped = state.flipped,
                    onSquareClick = onSquareClick,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = when (state.sideToMove) {
                        com.chessassistant.corechess.model.Color.WHITE -> "White to move"
                        else -> "Black to move"
                    },
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = state.openingName ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            state.outcome?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            state.bestMoveHint?.takeIf { h -> h.isNotEmpty() }?.let {
                Text(
                    text = "Engine suggests: $it",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(onClick = onNewGame, modifier = Modifier.weight(1f)) { Text("New") }
                Button(onClick = onUndo, modifier = Modifier.weight(1f)) { Text("Undo") }
                Button(onClick = onRedo, modifier = Modifier.weight(1f)) { Text("Redo") }
                OutlinedButton(onClick = onFlip, modifier = Modifier.weight(1f)) { Text("Flip") }
                Button(onClick = onSave, modifier = Modifier.weight(1f)) { Text("Save") }
            }
        }
    }
}