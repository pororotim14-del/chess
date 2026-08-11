package com.chessassistant.featuregames

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chessassistant.domain.model.GameId
import com.chessassistant.domain.model.GameSummary
import com.chessassistant.domain.repository.GameRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GamesViewModel @Inject constructor(
    private val gameRepository: GameRepository,
) : ViewModel() {

    val games: StateFlow<List<GameSummary>> =
        gameRepository.observeGames().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    fun delete(id: GameId) {
        viewModelScope.launch { gameRepository.deleteGame(id) }
    }
}