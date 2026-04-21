package com.example.chessrepertoiretrainer.core.chess.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.chessrepertoiretrainer.core.chess.controller.ChessBoardController
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.Side

@Composable
internal fun PromotionOverlay(
    state: ChessBoardController, board: Board
) {
    val promotion = state.pendingPromotion ?: return
    val side = board.getPiece(promotion.from).pieceSide
    val options = if (side == Side.WHITE) {
        listOf(
            Piece.WHITE_QUEEN, Piece.WHITE_ROOK, Piece.WHITE_BISHOP, Piece.WHITE_KNIGHT
        )
    }
    else {
        listOf(
            Piece.BLACK_QUEEN, Piece.BLACK_ROOK, Piece.BLACK_BISHOP, Piece.BLACK_KNIGHT
        )
    }
 
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ChessUiConstants.PromotionOverlay.scrimColor),
        contentAlignment = Alignment.Center
    ) {
        Row {
            options.forEach { promo ->
                Box(
                    modifier = Modifier
                        .size(ChessUiConstants.PromotionOverlay.pieceSize)
                        .clickable { state.promotePendingMove(promo) },
                    contentAlignment = Alignment.Center
                ) {
                    PieceDisplay(promo, modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}


