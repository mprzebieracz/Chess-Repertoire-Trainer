package com.example.chessrepertoiretrainer.core.chess.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import com.example.chessrepertoiretrainer.core.chess.controller.ChessBoardController
import com.example.chessrepertoiretrainer.core.chess.domain.Arrow
import com.github.bhlangonijr.chesslib.Square

@Composable
fun ChessboardUI(
    state: ChessBoardController,
    arrowDrawingMode: Boolean = false,
    onArrowDrawn: ((Arrow) -> Unit)? = null,
) {
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

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .onGloballyPositioned { boardSizePx = it.size.width.toFloat() }) {
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

        ArrowsLayer(
            arrows = state.arrows,
            squareSizePx = squareSizePx,
            isFlipped = state.isFlipped,
            modifier = Modifier.matchParentSize()
        )

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

        if (arrowDrawingMode && onArrowDrawn != null && squareSizePx > 0f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .pointerInput(squareSizePx, state.isFlipped) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            down.consume()
                            val startSquare = squareFromBoardOffset(
                                down.position.x, down.position.y, squareSizePx, state.isFlipped
                            )
                            var endPosition = down.position
                            do {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                change.consume()
                                endPosition = change.position
                                if (!change.pressed) break
                            } while (true)
                            val endSquare = squareFromBoardOffset(
                                endPosition.x, endPosition.y, squareSizePx, state.isFlipped
                            )
                            onArrowDrawn(Arrow(startSquare, endSquare))
                        }
                    }
            )
        }

        PromotionOverlay(state = state, board = board)
    }
}