package com.example.chessrepertoiretrainer.core.chess.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import com.example.chessrepertoiretrainer.core.chess.controller.ChessBoardController
import com.github.bhlangonijr.chesslib.Square

@Composable
fun ChessboardUI(state: ChessBoardController) {
    val board = state.getBoard()
    var boardSizePx by remember { mutableFloatStateOf(0f) }

    // Drag State
    var draggingSquare by remember { mutableStateOf<Square?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var initialTouchOffset by remember { mutableStateOf(Offset.Zero) }

    val legalMoves = remember(state.boardState, state.selectedSquare) {
        state.selectedSquare?.let { sq ->
            board.legalMoves().filter { it.from == sq }.map { it.to }
        } ?: emptyList()
    }

    Box(modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(1f)
        .onGloballyPositioned { boardSizePx = it.size.width.toFloat() }
        .shadow(elevation = ChessUiConstants.BoardFrame.shadowElevation,
                shape = ChessUiConstants.BoardFrame.shape)
        .clip(ChessUiConstants.BoardFrame.shape)
        .border(width = ChessUiConstants.BoardFrame.borderWidth,
                color = ChessUiConstants.BoardFrame.borderColor,
                shape = ChessUiConstants.BoardFrame.shape)) {
        val squareSizePx = if (boardSizePx > 0) boardSizePx / 8 else 0f

        ChessboardGrid(state = state,
                       board = board,
                       legalMoves = legalMoves,
                       draggingSquare = draggingSquare,
                       squareSizePx = squareSizePx,
                       onDragStart = { sq, offset ->
                           draggingSquare = sq
                           dragOffset = Offset.Zero
                           initialTouchOffset = offset
                       },
                       onDragUpdate = { offset -> dragOffset += offset },
                       onDragEnd = {
                           draggingSquare = null
                           dragOffset = Offset.Zero
                       })

        if (draggingSquare != null && squareSizePx > 0f) {
            DraggedPieceLayer(piece = board.getPiece(draggingSquare!!),
                              square = draggingSquare!!,
                              squareSizePx = squareSizePx,
                              isFlipped = state.isFlipped,
                              initialTouchOffset = initialTouchOffset,
                              dragOffset = dragOffset)
        }

        PromotionOverlay(state = state, board = board)
    }
}