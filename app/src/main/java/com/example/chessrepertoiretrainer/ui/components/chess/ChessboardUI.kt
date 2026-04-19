package com.example.chessrepertoiretrainer.ui.components.chess

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import com.github.bhlangonijr.chesslib.Square

@Composable
fun ChessboardUI(state: ChessBoardController) {
    val board = state.getBoard()
    var boardSizePx by remember { mutableFloatStateOf(0f) }

    // Drag State
    var draggingSquare by remember { mutableStateOf<Square?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var initialTouchOffset by remember { mutableStateOf(Offset.Zero) }

    // Pre-calculate legal moves once per state change
    val legalMoves = remember(state.boardState, state.selectedSquare) {
        state.selectedSquare?.let { sq ->
            board.legalMoves().filter { it.from == sq }.map { it.to }
        } ?: emptyList()
    }

    Box(
        modifier = Modifier.chessboardFrame { boardSizePx = it }) {
        val squareSizePx = if (boardSizePx > 0) boardSizePx / 8 else 0f

        ChessboardGrid(
            state = state,
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
            DraggedPieceLayer(
                piece = board.getPiece(draggingSquare!!),
                square = draggingSquare!!,
                squareSizePx = squareSizePx,
                isFlipped = state.isFlipped,
                initialTouchOffset = initialTouchOffset,
                dragOffset = dragOffset
            )
        }

        PromotionOverlay(state = state, board = board)
    }
}
