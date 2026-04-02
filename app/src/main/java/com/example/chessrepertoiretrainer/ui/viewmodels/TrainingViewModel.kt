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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TrainingViewModel(
    private val repertoireDao: RepertoireDao,
    private val savedStateHandle: SavedStateHandle
) : ViewModel(), ChessBoardState {
    private val chapterId: Int = savedStateHandle.get<String>("chapterId")?.toInt() ?: 0
    private val board = Board()

    override var boardState by mutableStateOf(board.fen)
        private set

    override var selectedSquare by mutableStateOf<Square?>(null)
        private set

    override var lastMove by mutableStateOf<Move?>(null)
        private set

    override var hoveredSquare by mutableStateOf<Square?>(null)

    private var _isFlipped by mutableStateOf(false)
    override val isFlipped: Boolean get() = _isFlipped

    private val _uiState = MutableStateFlow(TrainingUiState())
    val uiState: StateFlow<TrainingUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<TrainingEvent>()
    val events = _events.asSharedFlow()

    private var linesToTrain: MutableList<List<LineMove>> = mutableListOf()
    private var currentLine: List<LineMove>? = null
    private var currentMoveIndex = 0
    private var userColor: String = "White"

    init {
        loadChapterAndStartTraining()
    }

    private fun loadChapterAndStartTraining() {
        viewModelScope.launch {
            val chapter = repertoireDao.getChapterById(chapterId)
            val repertoire = chapter?.let { repertoireDao.getRepertoireById(it.repertoireId) }
            
            if (repertoire != null) {
                userColor = repertoire.color
                _isFlipped = userColor.equals("Black", ignoreCase = true)
            }

            repertoireDao.getLinesForChapter(chapterId).first().let { lines ->
                val allLinesMoves = mutableListOf<List<LineMove>>()
                for (line in lines) {
                    val moves = repertoireDao.getMovesForLine(line.id).first()
                    if (moves.isNotEmpty()) {
                        allLinesMoves.add(moves)
                    }
                }

                if (allLinesMoves.isNotEmpty()) {
                    linesToTrain = allLinesMoves.shuffled().toMutableList()
                    _uiState.value = _uiState.value.copy(
                        totalLines = linesToTrain.size,
                        completedLinesCount = 0,
                        status = "Starting training..."
                    )
                    loadNextLine()
                } else {
                    _uiState.value = _uiState.value.copy(status = "No moves found in this chapter.")
                }
            }
        }
    }

    private fun loadNextLine() {
        if (linesToTrain.isNotEmpty()) {
            currentLine = linesToTrain.removeAt(0)
            currentMoveIndex = 0
            _uiState.value = _uiState.value.copy(
                status = "New line!",
                completedLinesCount = _uiState.value.totalLines - linesToTrain.size - 1
            )
            resetBoardToStartOfLine()
        } else {
            _uiState.value = _uiState.value.copy(status = "Chapter complete!", isComplete = true)
        }
    }

    private fun resetBoardToStartOfLine() {
        board.loadFromFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")
        currentMoveIndex = 0
        updateBoardState()
        checkAndPlayOpponentMove()
    }

    private fun updateBoardState() {
        boardState = board.fen
        selectedSquare = null
        lastMove = null
    }

    private fun checkAndPlayOpponentMove() {
        val line = currentLine ?: return
        if (currentMoveIndex < line.size) {
            val isUserMove = isUserSide()
            if (!isUserMove) {
                viewModelScope.launch {
                    delay(500)
                    playRepertoireMove(line[currentMoveIndex])
                    currentMoveIndex++
                    checkAndPlayOpponentMove()
                }
            }
        }
    }

    private fun isUserSide(): Boolean {
        val sideToMove = board.sideToMove
        return if (userColor.equals("White", ignoreCase = true)) {
            sideToMove == com.github.bhlangonijr.chesslib.Side.WHITE
        } else {
            sideToMove == com.github.bhlangonijr.chesslib.Side.BLACK
        }
    }

    override fun getBoard(): Board = board

    override fun onSquareClick(square: Square) {
        if (!isUserSide() || _uiState.value.isComplete) return
        
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
            attemptUserMove(move)
        } else {
            selectedSquare = null
        }
    }

    private fun attemptUserMove(move: Move) {
        val line = currentLine ?: return
        if (currentMoveIndex >= line.size) return

        val expectedMove = line[currentMoveIndex]
        val moveSan = generateSan(move)

        if (moveSan == expectedMove.moveSan) {
            playRepertoireMove(expectedMove)
            currentMoveIndex++
            _uiState.value = _uiState.value.copy(status = "Correct!")
            
            if (currentMoveIndex >= line.size) {
                viewModelScope.launch {
                    delay(1000)
                    loadNextLine()
                }
            } else {
                checkAndPlayOpponentMove()
            }
        } else {
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(status = "Mistake! Try again.")
                _events.emit(TrainingEvent.Mistake)
                delay(1500)
                resetBoardToStartOfLine()
            }
        }
    }

    private fun playRepertoireMove(move: LineMove) {
        board.loadFromFen(move.fen)
        boardState = board.fen
        selectedSquare = null
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
        ): T = TrainingViewModel(repertoireDao, handle) as T
    }
}

data class TrainingUiState(
    val status: String = "Initializing...",
    val completedLinesCount: Int = 0,
    val totalLines: Int = 0,
    val isComplete: Boolean = false
)

sealed class TrainingEvent {
    object Mistake : TrainingEvent()
}
