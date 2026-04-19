package com.example.chessrepertoiretrainer.ui.components.chess

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.Square
import kotlin.math.roundToInt

@Composable
internal fun DraggedPieceLayer(
    piece: Piece, square: Square, squareSizePx: Float, isFlipped: Boolean, initialTouchOffset: Offset, dragOffset: Offset
) {
    val density = LocalDensity.current
    val visual = visualPosition(square, isFlipped)

    val startXPx = visual.file * squareSizePx
    val startYPx = visual.rank * squareSizePx
    val dragScale = ChessUiConstants.DragPreview.scale
    val sizeDp = with(density) { (squareSizePx * dragScale).toDp() }

    Box(
        modifier = Modifier
            .size(sizeDp)
            .offset {
                IntOffset(
                    (startXPx + initialTouchOffset.x + dragOffset.x - (squareSizePx * dragScale / 2)).roundToInt(),
                    (startYPx + initialTouchOffset.y + dragOffset.y - (squareSizePx * dragScale / 2)).roundToInt()
                )
            }, contentAlignment = Alignment.Center
    ) {
        PieceDisplay(piece, modifier = Modifier.fillMaxSize())
    }
}




