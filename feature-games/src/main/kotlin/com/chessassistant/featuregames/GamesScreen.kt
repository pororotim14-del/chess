package com.chessassistant.featuregames

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.chessassistant.domain.model.GameSummary

@Composable
fun GamesScreen(
    onGameClick: (Long) -> Unit,
    viewModel: GamesViewModel = hiltViewModel(),
) {
    val games by viewModel.games.collectAsState()

    Scaffold { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (games.isEmpty()) {
                Text(
                    text = "No saved games yet.",
                    modifier = Modifier.align(Alignment.Center),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(games, key = { it.id.value }) { game ->
                        GameRow(game, onOpen = { onGameClick(game.id.value) }, onDelete = { viewModel.delete(game.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun GameRow(game: GameSummary, onOpen: () -> Unit, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(
                text = "${game.whiteName} vs ${game.blackName}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
            )
            Text(
                text = "${game.moveCount} plies  ·  ${game.result}",
                style = MaterialTheme.typography.bodyMedium,
            )
            TextButton(onClick = onDelete) { Text("Delete") }
        }
    }
}