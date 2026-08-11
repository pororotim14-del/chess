package com.chessassistant.featureboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chessassistant.corechess.model.Color
import com.chessassistant.corechess.model.GameState
import com.chessassistant.corechess.model.Move
import com.chessassistant.corechess.model.Position
import com.chessassistant.corechess.notation.FenParser
import com.chessassistant.corechess.notation.San
import com.chessassistant.corechess.rules.GameStatus
import com.chessassistant.corechess.rules.MoveGenerator
import com.chessassistant.coreengine.analysis.AnalysisEngine
import com.chessassistant.coreengine.analysis.EngineBest
import com.chessassistant.domain.model.GameId
import com.chessassistant.domain.model.StoredGame
import com.chessassistant.domain.model.StoredMove
import com.chessassistant.domain.repository.AnalysisRepository
import com.chessassistant.domain.repository.AnalysisState
import com.chessassistant.domain.repository.GameRepository
import com.chessassistant.domain.repository.OpeningRepository
import com.chessassistant.domain.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BoardViewModel @Inject constructor(
    private val analysisRepository: AnalysisRepository,
    private val gameRepository: GameRepository,
    private val openingRepository: OpeningRepository,
    private val preferencesRepository: PreferencesRepository,
    private val engine: AnalysisEngine,
) : ViewModel() {

    private var game = GameState()
    private val sanHistory = mutableListOf<String>()

    private val _uiState = MutableStateFlow(BoardUiState.IDLE)
    val uiState: StateFlow<BoardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesRepository.prefs.collect { prefs ->
                _uiState.update { it.copy(flipped = prefs.board.flipped) }
            }
        }
        refresh()
    }

    fun onSquareClick(square: Int) {
        val state = _uiState.value
        val selected = state.selected
        when {
            selected == null -> {
                val piece = game.position.board[square] ?: return
                if (piece.color != game.position.sideToMove) return
                _uiState.update {
                    it.copy(selected = square, legalTargets = legalTargetsFrom(square))
                }
            }
            selected == square -> {
                _uiState.update { it.copy(selected = null, legalTargets = emptySet()) }
            }
            else -> {
                val from = selected
                val move = findMove(from, square)
                if (move != null && game.play(move)) {
                    appendSan(game, move)
                    refresh()
                    runEngineHint()
                } else {
                    val piece = game.position.board[square]
                    _uiState.update {
                        it.copy(
                            selected = if (piece?.color == game.position.sideToMove) square else null,
                            legalTargets = emptySet(),
                        )
                    }
                }
            }
        }
    }

    fun play(move: Move) {
        if (game.play(move)) {
            appendSan(game, move)
            refresh()
            runEngineHint()
        }
    }

    fun playUci(uci: String) {
        Move.fromUci(uci)?.let { play(it) }
    }

    fun undo() {
        if (game.undo() && sanHistory.isNotEmpty()) {
            sanHistory.removeAt(sanHistory.lastIndex)
            _uiState.update { it.copy(bestMoveHint = null) }
            refresh()
        }
    }

    fun redo() {
        if (game.canRedo) {
            game.redo()
            val move = game.playedMoves().lastOrNull() ?: return
            appendSan(game, move)
            refresh()
        }
    }

    fun newGame(fen: String = Position.START_FEN) {
        sanHistory.clear()
        game = GameState(fen)
        refresh()
    }

    fun loadGame(gameId: Long) {
        viewModelScope.launch {
            val stored = gameRepository.loadGame(GameId(gameId)) ?: return@launch
            sanHistory.clear()
            sanHistory += stored.moves.map { it.san }
            game = GameState(stored.initialFen)
            stored.moves.forEach { game.playUci(it.uci) }
            refresh()
            runEngineHint()
        }
    }

    fun flip() {
        _uiState.update { it.copy(flipped = !it.flipped) }
    }

    fun saveGame() {
        viewModelScope.launch {
            val moves = game.playedMoves().toList()
            if (moves.isEmpty()) return@launch
            var pos = FenParser.parse(game.startFen) ?: Position.STARTING
            val stored = moves.map { m ->
                StoredMove(
                    uci = m.uci,
                    san = San.format(pos, m).ifBlank { m.uci },
                    fen = pos.apply(m).toFen(),
                ).also { pos = pos.apply(m) }
            }
            gameRepository.saveGame(
                StoredGame(
                    id = GameId.NONE,
                    initialFen = game.startFen,
                    moves = stored,
                    whiteName = "You",
                    blackName = if (uiState.value.sideToMove == Color.WHITE) "Engine" else "Opponent",
                    result = uiState.value.outcome ?: "*",
                    createdAtEpochMs = System.currentTimeMillis(),
                ),
            )
        }
    }

    private fun appendSan(game: GameState, move: Move) {
        val idx = game.playedMoves().indexOf(move)
        if (idx != game.playedMoves().lastIndex) {
            // San history already consistent; rebuild to be safe.
            sanHistory.clear()
            var pos = FenParser.parse(game.startFen) ?: Position.STARTING
            for (m in game.playedMoves()) {
                sanHistory += San.format(pos, m).ifBlank { m.uci }
                pos = pos.apply(m)
            }
            return
        }
        var pos = FenParser.parse(game.startFen) ?: Position.STARTING
        for (m in game.playedMoves().dropLast(1)) pos = pos.apply(m)
        sanHistory += San.format(pos, move).ifBlank { move.uci }
    }

    private fun runEngineHint() {
        viewModelScope.launch {
            val best: EngineBest? = engine.bestMove(game.position)
            _uiState.update { it.copy(bestMoveHint = best?.move?.takeIf(String::isNotEmpty)) }
        }
    }

    private fun legalTargetsFrom(from: Int): Set<Int> =
        MoveGenerator.legalMoves(game.position)
            .asSequence()
            .filter { it.from == from }
            .map { it.to }
            .toSet()

    private fun findMove(from: Int, to: Int): Move? =
        MoveGenerator.legalMoves(game.position).firstOrNull { it.from == from && it.to == to }

    private fun refresh() {
        val pos = game.position
        val status = game.gameStatus()
        val checkSq = if (pos.isInCheck(pos.sideToMove)) {
            runCatching { pos.findKing(pos.sideToMove) }.getOrNull()
        } else {
            null
        }
        val opening = openingRepository.findOpening(sanHistory)
        _uiState.update {
            it.copy(
                fen = pos.toFen(),
                pieceSnapshot = pos.board.toList(),
                sideToMove = pos.sideToMove,
                lastMove = game.lastMove?.let { m -> m.from to m.to },
                kingInCheck = checkSq,
                status = status,
                outcome = outcomeFor(pos, status),
                openingName = opening?.name,
                selected = null,
                legalTargets = emptySet(),
                analysisReady = analysisRepository.state.value is AnalysisState.Result,
            )
        }
    }

    private fun outcomeFor(pos: Position, status: GameStatus): String? = when (status) {
        GameStatus.CHECKMATE -> if (pos.sideToMove == Color.WHITE) "Black wins" else "White wins"
        GameStatus.STALEMATE -> "Draw (stalemate)"
        else -> null
    }

    override fun onCleared() {
        analysisRepository.stop()
        engine.dispose()
    }
}