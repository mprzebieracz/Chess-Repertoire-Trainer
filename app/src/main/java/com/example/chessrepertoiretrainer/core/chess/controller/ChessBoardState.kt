package com.example.chessrepertoiretrainer.core.chess.controller

import com.example.chessrepertoiretrainer.core.chess.domain.Arrow
import com.example.chessrepertoiretrainer.core.chess.pgn.navigator.AppliedMove
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.Side
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.move.Move

data class PendingPromotion(val from: Square, val to: Square)

interface ChessBoardController {
    val boardState: String
    val selectedSquare: Square?
    val lastMove: Move?

    // Square currently hovered during drag operations.
    var hoveredSquare: Square?

    var markedSquare: Square?
    val isFlipped: Boolean

    var arrows: List<Arrow>

    /**
     * When null, board input is unrestricted (both sides can move).
     * When set to a [Side], only that side may initiate moves via user input.
     * Programmatic moves (e.g. training auto replies) should call [onMove] directly
     * and are not restricted by this flag.
     */
    var allowedMoveSide: Side?

    val pendingPromotion: PendingPromotion?
    fun getBoard(): Board
    fun onSquareClick(square: Square)
    fun onMove(move: Move)
    fun promotePendingMove(promotionPiece: Piece)
    fun loadPositionFromFen(fen: String, lastMove: Move? = null)
    fun resetBoard()
    fun flipBoard()
    fun orientForSide(side: Side)

    /** Called after a move is applied. Navigators subscribe here to receive move data. */
    var onMoveApplied: ((AppliedMove) -> Unit)?

    /**
     * Applies [move] to the board if legal and fires [onMoveApplied].
     * Returns the applied move data, or null if the move is illegal.
     */
    fun tryApplyMove(move: Move): AppliedMove?

}
