package com.example.chessrepertoiretrainer.core.chess.controller

import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.Side
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.move.Move

data class PendingPromotion(val from: Square, val to: Square)

interface ChessBoardController {
    val pgnState: String
    val boardState: String
    val selectedSquare: Square?
    val lastMove: Move?

    // Square currently hovered during drag operations.
    var hoveredSquare: Square?

    var markedSquare: Square?
    val isFlipped: Boolean

    /**
     * When null, board input is unrestricted (both sides can move).
     * When set to a [Side], only that side may initiate moves via user input.
     * Programmatic moves (e.g. training auto replies) should call [onMove] directly
     * and are not restricted by this flag.
     */
    var allowedMoveSide: Side?
    var onMoveListener: ((Move, String, String) -> Unit)?
    val pendingPromotion: PendingPromotion?
    fun getBoard(): Board
    fun onSquareClick(square: Square)
    fun onMove(move: Move)
    fun promotePendingMove(promotionPiece: Piece)
    fun navigateBack()
    fun navigateForward()
    fun loadPositionFromFen(fen: String)
    fun resetBoard()
    fun flipBoard()
}