package com.chessassistant.featuresettings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.chessassistant.domain.model.AppPrefs

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val prefs by viewModel.prefs.collectAsState()
    val uriHandler = LocalUriHandler.current

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Board", style = MaterialTheme.typography.titleLarge)
            ToggleRow("Flipped board", prefs.board.flipped) { viewModel.setFlipped(it) }
            ToggleRow("Highlight last move", prefs.board.highlightLastMove) { viewModel.setShowLastMove(it) }

            Text("Engine", style = MaterialTheme.typography.titleLarge)
            Text("Search depth: ${prefs.engine.depth}", style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = prefs.engine.depth.toFloat(),
                onValueChange = { viewModel.setEngineDepth(it.toInt()) },
                valueRange = 1f..20f,
                steps = 18,
                modifier = Modifier.fillMaxWidth(),
            )

            Text("Analysis", style = MaterialTheme.typography.titleLarge)
            ToggleRow("Show best-move hint", prefs.analysis.showBestMoveHint) { viewModel.setShowBestMoveHint(it) }

            Spacer(Modifier.weight(1f))
            
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("About TRX-CHESS", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = { uriHandler.openUri("https://t.me/SoloBanNoTrash") }) {
                    Text("Telegram: t.me/SoloBanNoTrash")
                }
                Text("By Troxzy", style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}