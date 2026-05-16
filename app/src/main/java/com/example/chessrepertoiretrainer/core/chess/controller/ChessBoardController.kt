package com.example.chessrepertoiretrainer.core.chess.controller

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.chessrepertoiretrainer.core.chess.domain.Arrow
import com.example.chessrepertoiretrainer.core.chess.domain.toSan
import com.example.chessrepertoiretrainer.core.chess.pgn.navigator.AppliedMove
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.Rank
import com.github.bhlangonijr.chesslib.Side
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.move.Move

class DefaultChessBoardController : ChessBoardController {

    companion object {
        private const val STARTING_POSITION_FEN =
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
    }

    private val board = Board()

    override var boardState by mutableStateOf(board.fen)
        private set
    override var selectedSquare by mutableStateOf<Square?>(null)
        private set
    override var lastMove by mutableStateOf<Move?>(null)
        private set
    override var hoveredSquare by mutableStateOf<Square?>(null)
    override var markedSquare by mutableStateOf<Square?>(null)
    override var isFlipped by mutableStateOf(false)
        private set
    override var arrows by mutableStateOf<List<Arrow>>(emptyList())

    override var pendingPromotion by mutableStateOf<PendingPromotion?>(null)
        private set

    override var allowedMoveSide: Side? = null
    override var onMoveApplied: ((AppliedMove) -> Unit)? = null

    override fun getBoard(): Board = board

    override fun onSquareClick(square: Square) {
        if (isInputBlockedBySideRestriction()) return

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
        val piece = board.getPiece(move.from)

        val isPawnPromotionMove =
            (piece == Piece.WHITE_PAWN && move.to.rank == Rank.RANK_8) ||
                    (piece == Piece.BLACK_PAWN && move.to.rank == Rank.RANK_1)

        if (isPawnPromotionMove) {
            val promotionMoves = board.legalMoves().filter { legal ->
                legal.from == move.from && legal.to == move.to && legal.promotion != Piece.NONE
            }
            if (promotionMoves.isNotEmpty()) {
                pendingPromotion = PendingPromotion(move.from, move.to)
                selectedSquare = null
                hoveredSquare = null
                markedSquare = null
                return
            }
        }

        val legalMoves = board.legalMoves()
        if (legalMoves.contains(move)) {
            applyMove(move)
        } else {
            val piece2 = board.getPiece(move.to)
            if (piece2 != Piece.NONE && piece2.pieceSide == board.sideToMove) {
                selectedSquare = move.to
            } else {
                selectedSquare = null
            }
        }
    }

    override fun promotePendingMove(promotionPiece: Piece) {
        val pending = pendingPromotion ?: return
        val promotionMove = board.legalMoves().firstOrNull { legal ->
            legal.from == pending.from && legal.to == pending.to && legal.promotion == promotionPiece
        } ?: return
        pendingPromotion = null
        applyMove(promotionMove)
    }

    override fun flipBoard() {
        isFlipped = !isFlipped
    }

    override fun orientForSide(side: Side) {
        if (side == Side.BLACK && !isFlipped) flipBoard()
        else if (side == Side.WHITE && isFlipped) flipBoard()
    }

    override fun resetBoard() {
        loadFenAndResetState(STARTING_POSITION_FEN)
    }

    override fun loadPositionFromFen(fen: String, lastMove: Move?) {
        loadFenAndResetState(fen)
        this.lastMove = lastMove
    }

    override fun tryApplyMove(move: Move): AppliedMove? {
        if (!board.legalMoves().contains(move)) return null
        val fenBefore = board.fen
        val san = board.toSan(move)
        applyMoveInternal(move, san, fenBefore)
        return AppliedMove(move, san, fenBefore, board.fen)
    }

    private fun applyMove(move: Move) {
        val fenBefore = board.fen
        val san = board.toSan(move)
        applyMoveInternal(move, san, fenBefore)
    }

    private fun applyMoveInternal(move: Move, san: String, fenBefore: String) {
        board.doMove(move)
        boardState = board.fen
        selectedSquare = null
        lastMove = move
        hoveredSquare = null
        markedSquare = null
        pendingPromotion = null
        onMoveApplied?.invoke(AppliedMove(move, san, fenBefore, board.fen))
    }

    private fun isInputBlockedBySideRestriction(): Boolean {
        val restriction = allowedMoveSide ?: return false
        return board.sideToMove != restriction
    }

    private fun loadFenAndResetState(fen: String) {
        board.loadFromFen(fen)
        boardState = board.fen
        selectedSquare = null
        lastMove = null
        hoveredSquare = null
        markedSquare = null
        pendingPromotion = null
    }
}
