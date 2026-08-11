package com.chessassistant.featuresettings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chessassistant.domain.model.AppPrefs
import com.chessassistant.domain.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {

    val prefs: StateFlow<AppPrefs> =
        preferencesRepository.prefs.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppPrefs(),
        )

    fun setFlipped(flipped: Boolean) {
        viewModelScope.launch {
            preferencesRepository.update { it.copy(board = it.board.copy(flipped = flipped)) }
        }
    }

    fun setShowLastMove(show: Boolean) {
        viewModelScope.launch {
            preferencesRepository.update { it.copy(board = it.board.copy(highlightLastMove = show)) }
        }
    }

    fun setEngineDepth(depth: Int) {
        viewModelScope.launch {
            preferencesRepository.update { it.copy(engine = it.engine.copy(depth = depth)) }
        }
    }

    fun setShowBestMoveHint(show: Boolean) {
        viewModelScope.launch {
            preferencesRepository.update { it.copy(analysis = it.analysis.copy(showBestMoveHint = show)) }
        }
    }
}