package com.example.chessrepertoiretrainer.feature.openingtree.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.chessrepertoiretrainer.core.chess.controller.DefaultChessBoardController
import com.example.chessrepertoiretrainer.core.chess.domain.findLegalMoveBySan
import com.example.chessrepertoiretrainer.core.chess.pgn.extract.PGNExtractor
import com.example.chessrepertoiretrainer.core.chess.pgn.navigator.GuidedLineNavigator
import com.example.chessrepertoiretrainer.core.database.entity.LineMove
import com.github.bhlangonijr.chesslib.Board
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class MasterGameViewerUiState(
    val isAtStart: Boolean = true,
    val isAtEnd: Boolean = false,
    val currentMoveLabel: String? = null,
    val totalMoves: Int = 0,
    val currentMoveIndex: Int = -1
)

class MasterGameViewerViewModel(pgn: String) : ViewModel() {

    val chessController = DefaultChessBoardController()

    private val _uiState = MutableStateFlow(MasterGameViewerUiState())
    val uiState: StateFlow<MasterGameViewerUiState> = _uiState.asStateFlow()

    private val navigator: GuidedLineNavigator

    init {
        val lineMoves = buildLineMoves(pgn)
        navigator = GuidedLineNavigator(lineMoves)
        chessController.onMoveApplied = null
        updateState()
    }

    fun onNext() {
        val nextSan = navigator.peekNextSan() ?: return
        val move = chessController.getBoard().findLegalMoveBySan(nextSan) ?: return
        val saved = chessController.onMoveApplied
        chessController.onMoveApplied = null
        chessController.onMove(move)
        chessController.onMoveApplied = saved
        navigator.goNext()
        updateState()
    }

    fun onPrevious() {
        if (navigator.isAtStart) return
        navigator.goPrevious()
        chessController.loadPositionFromFen(navigator.currentFen())
        updateState()
    }

    private fun updateState() {
        val node = navigator.currentNode()
        val label = if (node != null) {
            val parts = node.fenBefore.split(" ")
            val moveNum = parts.getOrNull(5)?.toIntOrNull() ?: 1
            val isWhite = parts.getOrNull(1) != "b"
            if (isWhite) "$moveNum. ${node.san}" else "$moveNum… ${node.san}"
        } else null
        _uiState.update {
            it.copy(
                isAtStart = navigator.isAtStart,
                isAtEnd = navigator.isAtEnd,
                currentMoveLabel = label,
                currentMoveIndex = navigator.currentMoveIndex
            )
        }
    }

    private fun buildLineMoves(pgn: String): List<LineMove> {
        val sans = PGNExtractor.extractSanMovesFromPgn(pgn)
        val result = mutableListOf<LineMove>()
        val board = Board()
        sans.forEachIndexed { index, san ->
            val move = board.findLegalMoveBySan(san) ?: return result
            board.doMove(move)
            result.add(
                LineMove(
                    id = 0,
                    lineId = 0,
                    moveIndex = index,
                    moveSan = san,
                    fen = board.fen,
                    comment = null,
                    arrows = ""
                )
            )
        }
        return result
    }

    class Factory(private val pgn: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MasterGameViewerViewModel(pgn) as T
    }
}
