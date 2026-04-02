package com.example.chessrepertoiretrainer.ui.components.chess

import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.move.Move

interface ChessBoardState {
    val boardState: String
    val selectedSquare: Square?
    val lastMove: Move?
    var hoveredSquare: Square?
    val isFlipped: Boolean
    fun getBoard(): Board
    fun onSquareClick(square: Square)
    fun onMove(move: Move)
}
