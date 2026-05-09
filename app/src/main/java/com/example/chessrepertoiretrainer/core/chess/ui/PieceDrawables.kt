package com.example.chessrepertoiretrainer.core.chess.ui

import com.example.chessrepertoiretrainer.R
import com.github.bhlangonijr.chesslib.Piece

internal fun pieceDrawableRes(piece: Piece): Int {
    return when (piece) {
        Piece.WHITE_PAWN -> R.drawable.piece_white_pawn
        Piece.WHITE_KNIGHT -> R.drawable.piece_white_knight
        Piece.WHITE_BISHOP -> R.drawable.piece_white_bishop
        Piece.WHITE_ROOK -> R.drawable.piece_white_rook
        Piece.WHITE_QUEEN -> R.drawable.piece_white_queen
        Piece.WHITE_KING -> R.drawable.piece_white_king
        Piece.BLACK_PAWN -> R.drawable.piece_black_pawn
        Piece.BLACK_KNIGHT -> R.drawable.piece_black_knight
        Piece.BLACK_BISHOP -> R.drawable.piece_black_bishop
        Piece.BLACK_ROOK -> R.drawable.piece_black_rook
        Piece.BLACK_QUEEN -> R.drawable.piece_black_queen
        Piece.BLACK_KING -> R.drawable.piece_black_king
        else -> 0
    }
}