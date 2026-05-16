package com.example.chessrepertoiretrainer.feature.repertoire.presentation.viewmodel

import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.chessrepertoiretrainer.core.chess.controller.DefaultChessBoardController
import com.example.chessrepertoiretrainer.core.chess.domain.findLegalMoveBySan
import com.example.chessrepertoiretrainer.core.chess.domain.toSide
import com.example.chessrepertoiretrainer.core.chess.pgn.navigator.LinearGameNavigator
import com.example.chessrepertoiretrainer.core.database.entity.LineMove
import com.example.chessrepertoiretrainer.core.engine.EngineAnalysis
import com.example.chessrepertoiretrainer.core.engine.EngineSearchState
import com.example.chessrepertoiretrainer.core.engine.StockfishEngine
import com.example.chessrepertoiretrainer.feature.repertoire.domain.RepertoireRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LineEditorViewModel(
    private val repertoireRepository: RepertoireRepository,
    savedStateHandle: SavedStateHandle,
    private val engine: StockfishEngine
) : ViewModel() {

    val lineId: Int = checkNotNull(savedStateHandle["lineId"])
    val chessController = DefaultChessBoardController()
    val navigator = LinearGameNavigator()

    private val _editingComment = MutableStateFlow<String?>(null)
    val editingComment: StateFlow<String?> = _editingComment.asStateFlow()

    private val _hasChanges = MutableStateFlow(false)
    val hasChanges: StateFlow<Boolean> = _hasChanges.asStateFlow()

    val dbMoves: StateFlow<List<LineMove>> = repertoireRepository.getMovesForLine(lineId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isEngineEnabled: StateFlow<Boolean> =
        engine.isEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    val engineAnalysis: StateFlow<EngineAnalysis?> =
        engine.analysis.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val engineSearchState: StateFlow<EngineSearchState> = engine.searchState.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(
            5_000
        ),
        EngineSearchState.IDLE
    )
    val engineError: StateFlow<String?> =
        engine.engineError.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        navigator.onPositionChanged = { fen, lm -> chessController.loadPositionFromFen(fen, lm) }

        viewModelScope.launch {
            val line = repertoireRepository.getLineById(lineId)
            if (line != null) {
                val chapter = repertoireRepository.getChapterById(line.chapterId)
                val repertoire =
                    chapter?.let { repertoireRepository.getRepertoireById(it.repertoireId) }
                val colorString = repertoire?.color ?: "White"

                chessController.orientForSide(colorString.toSide())
            }

            // Seed navigator by replaying stored moves through the controller.
            val moves = repertoireRepository.getMovesForLine(lineId).first()
            val board = chessController.getBoard()
            for (move in moves) {
                val legalMove = board.findLegalMoveBySan(move.moveSan) ?: break
                chessController.tryApplyMove(legalMove)?.let { navigator.onUserMove(it) }
            }

            // Wire persistence: new moves applied to the board are saved to the DB.
            chessController.onMoveApplied = { applied ->
                _editingComment.value = null
                _hasChanges.value = true
                viewModelScope.launch {
                    val nextIndex = dbMoves.value.size
                    repertoireRepository.insertLineMove(
                        LineMove(
                            lineId = lineId,
                            moveIndex = nextIndex,
                            moveSan = applied.san,
                            fen = applied.fenAfter,
                            comment = null,
                            arrows = null
                        )
                    )
                }
                navigator.onUserMove(applied)
            }

            viewModelScope.launch {
                snapshotFlow { chessController.boardState }.distinctUntilChanged().collect { fen ->
                    if (engine.isEnabled.value) engine.updatePosition(fen)
                }
            }
        }
    }

    fun resetToStart() {
        _editingComment.value = null
        val savedApplied = chessController.onMoveApplied
        chessController.onMoveApplied = null
        // Stop at index 0 (after first move), matching original behaviour.
        while (navigator.currentMoveIndex > 0) {
            navigator.goPrevious()
        }
        chessController.onMoveApplied = savedApplied
    }

    fun deleteLastMove() {
        viewModelScope.launch {
            val currentMoves = dbMoves.value
            if (currentMoves.isNotEmpty()) {
                repertoireRepository.deleteLineMove(currentMoves.last())
                _hasChanges.value = true
                val savedApplied = chessController.onMoveApplied
                chessController.onMoveApplied = null
                navigator.goPrevious()
                chessController.onMoveApplied = savedApplied
                _editingComment.value = null
            }
        }
    }

    fun startEditingComment(currentComment: String?) {
        _editingComment.value = currentComment ?: ""
    }

    fun onCommentTextChange(text: String) {
        _editingComment.value = text
    }

    fun saveComment(fen: String) {
        viewModelScope.launch {
            val target = dbMoves.value.firstOrNull { it.fen == fen } ?: return@launch
            val text = _editingComment.value ?: return@launch
            repertoireRepository.updateLineMove(target.copy(comment = text.ifBlank { null }))
            _editingComment.value = null
        }
    }

    fun cancelEditingComment() {
        _editingComment.value = null
    }

    fun toggleEngine() {
        if (engine.isEnabled.value) engine.disable()
        else engine.enable(chessController.boardState)
    }

    fun analyzeDeeper() = engine.analyzeDeeper()

    override fun onCleared() {
        super.onCleared()
        engine.disable()
    }

    class Factory(
        private val repertoireRepository: RepertoireRepository,
        private val engine: StockfishEngine
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val handle = extras.createSavedStateHandle()
            return LineEditorViewModel(repertoireRepository, handle, engine) as T
        }
    }
}