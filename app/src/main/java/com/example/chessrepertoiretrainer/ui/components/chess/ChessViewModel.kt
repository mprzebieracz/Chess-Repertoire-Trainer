package com.example.chessrepertoiretrainer.ui.components.chess

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.move.Move
import com.github.bhlangonijr.chesslib.Piece

class ChessViewModel : ViewModel(), ChessBoardState {
    private val board = Board()
    private val fullMoveHistory = mutableListOf<Move>()
    private val fullSanHistory = mutableListOf<String>()
    private var currentPositionIndex = -1 // Index in fullMoveHistory, -1 is start
    
    override var boardState by mutableStateOf(board.fen)
        private set

    override var selectedSquare by mutableStateOf<Square?>(null)
        private set

    override var lastMove by mutableStateOf<Move?>(null)
        private set

    override var hoveredSquare by mutableStateOf<Square?>(null)

    override var isFlipped by mutableStateOf(false)
        private set

    var pgnState by mutableStateOf("")
        private set

    override fun getBoard(): Board = board

    override fun onSquareClick(square: Square) {
        val currentSelected = selectedSquare
        if (currentSelected == null) {
            val piece = board.getPiece(square)
            if (piece != Piece.NONE && 
                piece.pieceSide == board.sideToMove) {
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
            // If we are navigating history and make a new move, truncate the future
            if (currentPositionIndex < fullMoveHistory.size - 1) {
                val itemsToRemove = fullMoveHistory.size - 1 - currentPositionIndex
                repeat(itemsToRemove) {
                    fullMoveHistory.removeAt(fullMoveHistory.size - 1)
                    fullSanHistory.removeAt(fullSanHistory.size - 1)
                }
            }

            val san = generateSan(move)
            board.doMove(move)
            fullMoveHistory.add(move)
            fullSanHistory.add(san)
            currentPositionIndex++
            
            updatePgn()
            boardState = board.fen
            selectedSquare = null
            lastMove = move
        } else {
            val piece = board.getPiece(move.to)
            if (piece != Piece.NONE && piece.pieceSide == board.sideToMove) {
                selectedSquare = move.to
            } else {
                selectedSquare = null
            }
        }
    }

    fun navigateBack() {
        if (currentPositionIndex >= 0) {
            board.undoMove()
            currentPositionIndex--
            updateAfterNavigation()
        }
    }

    fun navigateForward() {
        if (currentPositionIndex < fullMoveHistory.size - 1) {
            currentPositionIndex++
            val nextMove = fullMoveHistory[currentPositionIndex]
            board.doMove(nextMove)
            updateAfterNavigation()
        }
    }

    private fun updateAfterNavigation() {
        boardState = board.fen
        selectedSquare = null
        lastMove = if (currentPositionIndex >= 0) fullMoveHistory[currentPositionIndex] else null
        updatePgn()
    }

    private fun updatePgn() {
        val sb = StringBuilder()
        for (i in fullSanHistory.indices) {
            if (i % 2 == 0) sb.append("${(i / 2) + 1}. ")
            if (i == currentPositionIndex) {
                sb.append("[").append(fullSanHistory[i]).append("] ")
            } else {
                sb.append(fullSanHistory[i]).append(" ")
            }
        }
        pgnState = sb.toString().trim()
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

    fun flipBoard() {
        isFlipped = !isFlipped
    }

    fun resetBoard() {
        board.loadFromFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")
        fullMoveHistory.clear()
        fullSanHistory.clear()
        currentPositionIndex = -1
        pgnState = ""
        boardState = board.fen
        selectedSquare = null
        lastMove = null
        hoveredSquare = null
    }
}
