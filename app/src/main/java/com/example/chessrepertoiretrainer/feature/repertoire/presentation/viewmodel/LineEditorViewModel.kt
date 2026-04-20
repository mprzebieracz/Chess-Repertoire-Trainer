package com.example.chessrepertoiretrainer.feature.repertoire.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.chessrepertoiretrainer.core.database.dao.RepertoireDao
import com.example.chessrepertoiretrainer.core.database.entity.LineMove
import com.example.chessrepertoiretrainer.core.chess.controller.DefaultChessBoardController
import com.example.chessrepertoiretrainer.core.chess.domain.toSan
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LineEditorViewModel(
    private val repertoireDao: RepertoireDao, savedStateHandle: SavedStateHandle
) : ViewModel() {

    val lineId: Int = checkNotNull(savedStateHandle["lineId"])
    val chessController = DefaultChessBoardController()

    val dbMoves: StateFlow<List<LineMove>> =
        repertoireDao.getMovesForLine(lineId).stateIn(viewModelScope, SharingStarted.Companion.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            val line = repertoireDao.getLineById(lineId)
            if (line != null) {
                val chapter = repertoireDao.getChapterById(line.chapterId)
                val repertoire = chapter?.let { repertoireDao.getRepertoireById(it.repertoireId) }
                val colorString = repertoire?.color ?: "White"

                if (colorString.equals("Black", ignoreCase = true) && !chessController.isFlipped) {
                    chessController.flipBoard()
                }
            }

            val moves = repertoireDao.getMovesForLine(lineId).first()
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
                viewModelScope.launch {
                    val nextIndex = dbMoves.value.size
                    repertoireDao.insertLineMove(
                        LineMove(
                            lineId = lineId, moveIndex = nextIndex, moveSan = san, fen = fen, comment = null, arrows = null
                        )
                    )
                }
            }
        }
    }

    fun undoDbMove() {
        viewModelScope.launch {
            val currentMoves = dbMoves.value
            if (currentMoves.isNotEmpty()) {
                repertoireDao.deleteLineMove(currentMoves.last())

                val tempListener = chessController.onMoveListener
                chessController.onMoveListener = null
                chessController.navigateBack()
                chessController.onMoveListener = tempListener
            }
        }
    }

    class Factory(private val repertoireDao: RepertoireDao) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val handle = extras.createSavedStateHandle()
            return LineEditorViewModel(repertoireDao, handle) as T
        }
    }
}