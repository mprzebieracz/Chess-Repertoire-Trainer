package com.example.chessrepertoiretrainer.ui.components.chess

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
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.move.Move
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MoveNode(
    val move: LineMove,
    val children: MutableList<MoveNode> = mutableListOf()
)

class RepertoireEditorViewModel(
    private val repertoireDao: RepertoireDao,
    private val savedStateHandle: SavedStateHandle
) : ViewModel(), ChessBoardState {
    private val repertoireId: Int = savedStateHandle.get<String>("repertoireId")?.toInt() ?: 0
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

    private val _pgn = MutableStateFlow("")
    val pgn: StateFlow<String> = _pgn.asStateFlow()

    private val _currentPath = MutableStateFlow<List<LineMove>>(emptyList())
    val currentPath: StateFlow<List<LineMove>> = _currentPath.asStateFlow()

    private val _availableMoves = MutableStateFlow<List<LineMove>>(emptyList())
    val availableMoves: StateFlow<List<LineMove>> = _availableMoves.asStateFlow()

    private val _moveTree = MutableStateFlow<List<MoveNode>>(emptyList())
    val moveTree: StateFlow<List<MoveNode>> = _moveTree.asStateFlow()

    private var currentMoveId: Long? = null
    private var allMoves: List<LineMove> = emptyList()

    init {
        loadRepertoireInfo()
        loadMoves()
    }

    private fun loadRepertoireInfo() {
        viewModelScope.launch {
            repertoireDao.getAllRepertoires().collect { repertoires ->
                val repertoire = repertoires.find { it.id == repertoireId }
                if (repertoire != null) {
                    isFlipped = repertoire.color.equals("Black", ignoreCase = true)
                }
            }
        }
    }

    private fun loadMoves() {
        viewModelScope.launch {
            repertoireDao.getMovesForRepertoire(repertoireId).collect { moves ->
                allMoves = moves
                buildTree()
                updateState()
            }
        }
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

            val move = Move(currentSelected, square)
            onMove(move)
        }
    }

    override fun onMove(move: Move) {
        if (board.legalMoves().contains(move)) {
            makeMove(move)
        } else {
            val piece = board.getPiece(move.to)
            if (piece != Piece.NONE && piece.pieceSide == board.sideToMove) {
                selectedSquare = move.to
            } else {
                selectedSquare = null
            }
        }
    }

    private fun makeMove(move: Move) {
        // This is a legacy view that doesn't fit the new Line structure well.
        // It's kept to avoid compilation errors but won't work correctly with the new schema.
    }

    fun selectMove(move: LineMove?) {
        currentMoveId = move?.id
        updateBoardToCurrentPosition()
    }

    fun flipBoard() {
        isFlipped = !isFlipped
    }

    fun deleteCurrentMove() {
        // Legacy
    }

    private fun updateBoardToCurrentPosition() {
        val currentMove = allMoves.find { it.id == currentMoveId }
        val fen = currentMove?.fen ?: "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
        board.loadFromFen(fen)
        boardState = board.fen
        selectedSquare = null
        lastMove = null
        updateState()
    }

    private fun buildTree() {
        // Legacy
    }

    private fun updateState() {
        // Legacy
    }

    private fun generateSan(move: Move): String {
        val piece = board.getPiece(move.from)
        val isCapture = board.getPiece(move.to) != Piece.NONE
        
        if (piece == Piece.WHITE_KING || piece == Piece.BLACK_KING) {
            if (move.from == Square.E1 && move.to == Square.G1) return "O-O"
            if (move.from == Square.E1 && move.to == Square.C1) return "O-O-O"
            if (move.from == Square.E8 && move.to == Square.G8) return "O-O"
            if (move.from == Square.E8 && move.to == Square.C8) return "O-O-O"
        }

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
        } else {
            "$piecePrefix$captureSign$destination"
        }
    }

    private fun formatPgn(moves: List<String>): String {
        val sb = StringBuilder()
        for (i in moves.indices) {
            if (i % 2 == 0) sb.append("${(i / 2) + 1}. ")
            sb.append(moves[i]).append(" ")
        }
        return sb.toString().trim()
    }

    fun navigateBack() {
        // Legacy
    }

    fun navigateForward() {
        // Legacy
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
        ): T {
            return RepertoireEditorViewModel(repertoireDao, handle) as T
        }
    }
}
