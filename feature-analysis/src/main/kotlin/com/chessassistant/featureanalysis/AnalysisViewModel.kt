package com.chessassistant.featureanalysis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chessassistant.corechess.notation.FenParser
import com.chessassistant.domain.model.PositionAnalysis
import com.chessassistant.domain.repository.AnalysisRepository
import com.chessassistant.domain.repository.AnalysisState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AnalysisViewModel @Inject constructor(
    private val analysisRepository: AnalysisRepository,
) : ViewModel() {

    val state: StateFlow<AnalysisState> = analysisRepository.state

    private val _currentFen = MutableStateFlow<String?>(null)
    val currentFen: StateFlow<String?> = _currentFen.asStateFlow()

    fun analyze(fen: String) {
        val position = FenParser.parse(fen) ?: return
        _currentFen.value = fen
        analysisRepository.analyze(position)
    }

    fun analyzeCurrent() {
        currentFen.value?.let { analyze(it) }
    }

    fun stop() {
        analysisRepository.stop()
    }

    fun ratio(analysis: PositionAnalysis): Float =
        analysisRepository.evaluationRatio(analysis)

    override fun onCleared() {
        analysisRepository.stop()
    }
}