package com.example.chessrepertoiretrainer.ui.components.chess

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.Rank
import com.github.bhlangonijr.chesslib.Side
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.move.Move

class DefaultChessBoardController(
    override var onMoveListener: ((Move, String, String) -> Unit)? = null
) : ChessBoardController {

    private val board = Board()
    private val fullMoveHistory = mutableListOf<Move>()
    private val fullSanHistory = mutableListOf<String>()
    private var currentPositionIndex = -1

    override var boardState by mutableStateOf(board.fen)
        private set
    override var selectedSquare by mutableStateOf<Square?>(null)
        private set
    override var lastMove by mutableStateOf<Move?>(null)
        private set
    override var hoveredSquare by mutableStateOf<Square?>(null)
    override var isFlipped by mutableStateOf(false)
        private set

    override var pgnState by mutableStateOf("")
        private set

    override var pendingPromotion by mutableStateOf<PendingPromotion?>(null)
        private set

    override var allowedMoveSide: Side? = null

    override fun getBoard(): Board = board

    override fun onSquareClick(square: Square) {
        // In training/play mode we may want to restrict user input to only one side.
        // When allowedMoveSide is set, ignore clicks while it's the opponent's turn.
        val restriction = allowedMoveSide
        if (restriction != null && board.sideToMove != restriction) {
            return
        }

        val currentSelected = selectedSquare
        if (currentSelected == null) {
            val piece = board.getPiece(square)
            if (piece != Piece.NONE &&
                piece.pieceSide == board.sideToMove
            ) {
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
        val piece = board.getPiece(move.from)

        val isPawnPromotionMove =
            (piece == Piece.WHITE_PAWN && move.to.rank == Rank.RANK_8) ||
                    (piece == Piece.BLACK_PAWN && move.to.rank == Rank.RANK_1)

        if (isPawnPromotionMove) {
            val promotionMoves = board.legalMoves().filter { legal ->
                legal.from == move.from &&
                        legal.to == move.to &&
                        legal.promotion != Piece.NONE
            }

            if (promotionMoves.isNotEmpty()) {
                pendingPromotion = PendingPromotion(move.from, move.to)
                selectedSquare = null
                hoveredSquare = null
                return
            }
        }

        if (board.legalMoves().contains(move)) {
            // If there is an existing move in the history from this position that
            // matches the move just played, simply advance along the existing
            // line instead of starting a new branch (which would truncate the
            // future moves and notify listeners).
            if (currentPositionIndex < fullMoveHistory.size - 1) {
                val nextRecordedMove = fullMoveHistory[currentPositionIndex + 1]
                val isSameAsNextRecorded =
                    nextRecordedMove.from == move.from &&
                            nextRecordedMove.to == move.to &&
                            nextRecordedMove.promotion == move.promotion

                if (isSameAsNextRecorded) {
                    navigateForward()
                    return
                }
            }

            applyMove(move)
        } else {
            val piece = board.getPiece(move.to)
            if (piece != Piece.NONE && piece.pieceSide == board.sideToMove) {
                selectedSquare = move.to
            } else {
                selectedSquare = null
            }
        }
    }

    override fun navigateBack() {
        if (currentPositionIndex >= 0) {
            board.undoMove()
            currentPositionIndex--
            updateAfterNavigation()
        }
    }

    override fun promotePendingMove(promotionPiece: Piece) {
        val pending = pendingPromotion ?: return

        val promotionMove = board.legalMoves().firstOrNull { legal ->
            legal.from == pending.from &&
                    legal.to == pending.to &&
                    legal.promotion == promotionPiece
        } ?: return

        pendingPromotion = null

        // Similar to normal moves, if the chosen promotion move is already the
        // next move in the current PGN line, just advance along that line
        // instead of creating a new branch.
        if (currentPositionIndex < fullMoveHistory.size - 1) {
            val nextRecordedMove = fullMoveHistory[currentPositionIndex + 1]
            val isSameAsNextRecorded =
                nextRecordedMove.from == promotionMove.from &&
                        nextRecordedMove.to == promotionMove.to &&
                        nextRecordedMove.promotion == promotionMove.promotion

            if (isSameAsNextRecorded) {
                navigateForward()
                return
            }
        }

        applyMove(promotionMove)
    }

    override fun navigateForward() {
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

    private fun applyMove(move: Move) {
        if (currentPositionIndex < fullMoveHistory.size - 1) {
            val itemsToRemove = fullMoveHistory.size - 1 - currentPositionIndex
            repeat(itemsToRemove) {
                fullMoveHistory.removeAt(fullMoveHistory.lastIndex)
                fullSanHistory.removeAt(fullSanHistory.lastIndex)
            }
        }

        val san = board.toSan(move)
        board.doMove(move)

        fullMoveHistory.add(move)
        fullSanHistory.add(san)
        currentPositionIndex++

        updatePgn()
        boardState = board.fen
        selectedSquare = null
        lastMove = move

        onMoveListener?.invoke(move, san, board.fen)
    }

    private fun updatePgn() {
        if (fullSanHistory.isEmpty()) {
            pgnState = ""
            return
        }

        val sb = StringBuilder()
        for (i in fullSanHistory.indices) {
            if (i % 2 == 0) {
                sb.append("${(i / 2) + 1}. ")
            }

            val san = fullSanHistory[i]
            if (i == currentPositionIndex && currentPositionIndex >= 0) {
                sb.append("[").append(san).append("] ")
            } else {
                sb.append(san).append(" ")
            }
        }

        pgnState = sb.toString().trim()
    }

    override fun flipBoard() {
        isFlipped = !isFlipped
    }

    override fun resetBoard() {
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

    fun loadFen(fen: String) {
        board.loadFromFen(fen)
        boardState = board.fen
    }
}