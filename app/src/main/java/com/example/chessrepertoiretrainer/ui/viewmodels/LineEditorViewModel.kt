package com.example.chessrepertoiretrainer.ui.viewmodels

import android.os.Bundle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.savedstate.SavedStateRegistryOwner
import com.example.chessrepertoiretrainer.data.LineMove
import com.example.chessrepertoiretrainer.data.RepertoireDao
import com.example.chessrepertoiretrainer.ui.components.chess.ChessBoardState
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.move.Move
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class LineEditorViewModel(
    private val repertoireDao: RepertoireDao,
    private val savedStateHandle: SavedStateHandle
) : ViewModel(), ChessBoardState {
    val lineId: Int = savedStateHandle.get<String>("lineId")?.toInt() ?: 0
    private val board = Board()

    override var boardState by mutableStateOf(board.fen)
        private set

    override var selectedSquare by mutableStateOf<Square?>(null)
        private set

    override var lastMove by mutableStateOf<Move?>(null)
        private set

    override var hoveredSquare by mutableStateOf<Square?>(null)

    override var isFlipped by mutableStateOf(false)
        private set

    private val _moves = MutableStateFlow<List<LineMove>>(emptyList())
    val moves: StateFlow<List<LineMove>> = _moves.asStateFlow()

    init {
        loadMoves()
        loadRepertoireInfo()
    }

    private fun loadMoves() {
        viewModelScope.launch {
            repertoireDao.getMovesForLine(lineId).collect { lineMoves ->
                _moves.value = lineMoves
                updateBoardToLastMove(lineMoves)
            }
        }
    }

    private fun loadRepertoireInfo() {
        viewModelScope.launch {
            val line = repertoireDao.getLineById(lineId)
            val chapter = line?.let { repertoireDao.getChapterById(it.chapterId) }
            val repertoire = chapter?.let { repertoireDao.getRepertoireById(it.repertoireId) }
            if (repertoire != null) {
                isFlipped = repertoire.color.equals("Black", ignoreCase = true)
            }
        }
    }

    private fun updateBoardToLastMove(lineMoves: List<LineMove>) {
        if (lineMoves.isEmpty()) {
            board.loadFromFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")
        } else {
            board.loadFromFen(lineMoves.last().fen)
        }
        boardState = board.fen
        lastMove = null // Could potentially store the last move as well if needed
    }

    override fun getBoard(): Board = board

    override fun onSquareClick(square: Square) {
        val currentSelected = selectedSquare
        if (currentSelected == null) {
            val piece = board.getPiece(square)
            if (piece != Piece.NONE && piece.pieceSide == board.sideToMove) {
                selectedSquare = square
            }
        } else {
            if (currentSelected == square) {
                selectedSquare = null
                return
            }
            onMove(Move(currentSelected, square))
        }
    }

    override fun onMove(move: Move) {
        if (board.legalMoves().contains(move)) {
            makeMove(move)
        } else {
            selectedSquare = null
        }
    }

    private fun makeMove(move: Move) {
        val moveSan = generateSan(move)
        board.doMove(move)
        val positionFen = board.fen
        
        viewModelScope.launch {
            val currentIndex = _moves.value.size
            val moveEntity = LineMove(
                lineId = lineId,
                moveIndex = currentIndex,
                moveSan = moveSan,
                fen = positionFen
            )
            repertoireDao.insertLineMove(moveEntity)
            boardState = positionFen
            selectedSquare = null
        }
    }

    fun deleteLastMove() {
        viewModelScope.launch {
            repertoireDao.deleteLastMoveForLine(lineId)
        }
    }

    private fun generateSan(move: Move): String {
        val piece = board.getPiece(move.from)
        val isCapture = board.getPiece(move.to) != Piece.NONE
        val piecePrefix = when (piece) {
            Piece.WHITE_KNIGHT, Piece.BLACK_KNIGHT -> "N"
            Piece.WHITE_BISHOP, Piece.BLACK_BISHOP -> "B"
            Piece.WHITE_ROOK, Piece.BLACK_ROOK -> "R"
            Piece.WHITE_QUEEN, Piece.BLACK_QUEEN -> "Q"
            Piece.WHITE_KING, Piece.BLACK_KING -> "K"
            else -> ""
        }
        val destination = move.to.toString().lowercase()
        val captureSign = if (isCapture) "x" else ""
        return if (piecePrefix == "") {
            if (isCapture) "${move.from.toString().lowercase()[0]}x$destination" else destination
        } else "$piecePrefix$captureSign$destination"
    }

    class Factory(
        private val repertoireDao: RepertoireDao,
        owner: SavedStateRegistryOwner,
        defaultArgs: Bundle? = null
    ) : AbstractSavedStateViewModelFactory(owner, defaultArgs) {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(
            key: String,
            modelClass: Class<T>,
            handle: SavedStateHandle
        ): T = LineEditorViewModel(repertoireDao, handle) as T
    }
}
