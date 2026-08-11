package com.chessassistant.featureanalysis

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.chessassistant.corechess.notation.FenParser
import com.chessassistant.coreui.components.ChessBoard
import com.chessassistant.domain.model.PositionAnalysis
import com.chessassistant.domain.repository.AnalysisState

@Composable
fun AnalysisScreen(
    initialFen: String? = null,
    viewModel: AnalysisViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(initialFen) {
        if (initialFen != null) viewModel.analyze(initialFen)
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (val s = state) {
                is AnalysisState.Idle -> Text("No analysis yet.")
                is AnalysisState.Analyzing -> Text("Analyzing...")
                is AnalysisState.Failed -> Text("Analysis failed: ${s.reason}")
                is AnalysisState.Result -> AnalysisContent(s.analysis, viewModel.ratio(s.analysis))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { viewModel.analyzeCurrent() }) { Text("Analyze") }
                OutlinedButton(onClick = { viewModel.stop() }) { Text("Stop") }
            }
        }
    }
}

@Composable
private fun AnalysisContent(analysis: PositionAnalysis, ratio: Float) {
    val pos = FenParser.parse(analysis.fen)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (pos != null) {
            ChessBoard(
                board = pos.board,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        EvaluationBar(ratio)
        Text("Evaluation: ${analysis.evaluation}", style = MaterialTheme.typography.titleMedium)
        val line = analysis.bestLine
        if (line.isNotEmpty()) {
            Text(
                text = "Best line: " + line.joinToString(" ") { it.san },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun EvaluationBar(ratio: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(18.dp)
            .background(Color(0xFF37474F)),
    ) {
        val whiteFraction = (ratio + 1f) / 2f
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(whiteFraction.coerceIn(0f, 1f))
                .background(Color.White)
                .align(Alignment.CenterStart),
        )
    }
}