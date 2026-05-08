package com.example.chessrepertoiretrainer.feature.repertoire.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.chessrepertoiretrainer.core.chess.controller.DefaultChessBoardController
import com.example.chessrepertoiretrainer.core.chess.domain.toSan
import com.example.chessrepertoiretrainer.core.database.entity.LineMove
import com.example.chessrepertoiretrainer.feature.repertoire.domain.RepertoireRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LineEditorViewModel(
    private val repertoireRepository: RepertoireRepository, savedStateHandle: SavedStateHandle
) : ViewModel() {

    val lineId: Int = checkNotNull(savedStateHandle["lineId"])
    val chessController = DefaultChessBoardController()

    private val _editingComment = MutableStateFlow<String?>(null)
    val editingComment: StateFlow<String?> = _editingComment.asStateFlow()

    private val _hasChanges = MutableStateFlow(false)
    val hasChanges: StateFlow<Boolean> = _hasChanges.asStateFlow()

    val dbMoves: StateFlow<List<LineMove>> =
        repertoireRepository.getMovesForLine(lineId).stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    init {
        viewModelScope.launch {
            val line = repertoireRepository.getLineById(lineId)
            if (line != null) {
                val chapter = repertoireRepository.getChapterById(line.chapterId)
                val repertoire = chapter?.let { repertoireRepository.getRepertoireById(it.repertoireId) }
                val colorString = repertoire?.color ?: "White"

                if (colorString.equals("Black", ignoreCase = true) && !chessController.isFlipped) {
                    chessController.flipBoard()
                }
            }

            val moves = repertoireRepository.getMovesForLine(lineId).first()
            val board = chessController.getBoard()

            moves.forEach { savedMove ->
                val legalMove = board.legalMoves().firstOrNull { move ->
                    board.toSan(move) == savedMove.moveSan
                }
                if (legalMove != null) {
                    chessController.onMove(legalMove)
                }
            }

            chessController.onMoveListener = { _, san, fen ->
                _editingComment.value = null
                _hasChanges.value = true
                viewModelScope.launch {
                    val nextIndex = dbMoves.value.size
                    repertoireRepository.insertLineMove(
                        LineMove(
                            lineId = lineId, moveIndex = nextIndex, moveSan = san, fen = fen,
                            comment = null, arrows = null
                        )
                    )
                }
            }
        }
    }

    fun resetToStart() {
        val tempListener = chessController.onMoveListener
        chessController.onMoveListener = null
        _editingComment.value = null
        while (chessController.currentMoveIndex > 0) {
            chessController.navigateBack()
        }
        chessController.onMoveListener = tempListener
    }

    fun deleteLastMove() {
        viewModelScope.launch {
            val currentMoves = dbMoves.value
            if (currentMoves.isNotEmpty()) {
                repertoireRepository.deleteLineMove(currentMoves.last())
                _hasChanges.value = true

                val tempListener = chessController.onMoveListener
                chessController.onMoveListener = null
                chessController.navigateBack()
                chessController.onMoveListener = tempListener
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

    class Factory(private val repertoireRepository: RepertoireRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val handle = extras.createSavedStateHandle()
            return LineEditorViewModel(repertoireRepository, handle) as T
        }
    }
}
