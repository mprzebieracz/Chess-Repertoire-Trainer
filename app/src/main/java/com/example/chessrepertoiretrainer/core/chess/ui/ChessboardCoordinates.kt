package com.example.chessrepertoiretrainer.core.chess.ui

import androidx.compose.ui.geometry.Offset
import com.github.bhlangonijr.chesslib.Square

internal data class VisualSquarePosition(val file: Int, val rank: Int)

internal fun visualPosition(square: Square, isFlipped: Boolean): VisualSquarePosition {
    val visualFile = if (isFlipped) 7 - square.file.ordinal else square.file.ordinal
    val visualRank = if (isFlipped) square.rank.ordinal else 7 - square.rank.ordinal
    return VisualSquarePosition(file = visualFile, rank = visualRank)
}

internal fun hoveredSquareFromPointer(
    squareSizePx: Float,
    isFlipped: Boolean,
    rankIndex: Int,
    fileIndex: Int,
    pointerX: Float,
    pointerY: Float
): Square {
    val currentX = (if (isFlipped) 7 - fileIndex else fileIndex) * squareSizePx + pointerX
    val currentY = (if (isFlipped) rankIndex else 7 - rankIndex) * squareSizePx + pointerY

    val targetFile =
        (currentX / squareSizePx).toInt().coerceIn(0, 7).let { if (isFlipped) 7 - it else it }
    val targetRank =
        (currentY / squareSizePx).toInt().coerceIn(0, 7).let { if (isFlipped) it else 7 - it }

    return Square.entries[targetRank * 8 + targetFile]
}

internal fun squareCenterPx(square: Square, squareSizePx: Float, isFlipped: Boolean): Offset {
    val vp = visualPosition(square, isFlipped)
    return Offset(
        x = vp.file * squareSizePx + squareSizePx / 2f,
        y = vp.rank * squareSizePx + squareSizePx / 2f
    )
}

internal fun squareFromBoardOffset(
    x: Float,
    y: Float,
    squareSizePx: Float,
    isFlipped: Boolean
): Square {
    val fileIdx = (x / squareSizePx).toInt().coerceIn(0, 7)
    val rankIdx = (y / squareSizePx).toInt().coerceIn(0, 7)
    val file = if (isFlipped) 7 - fileIdx else fileIdx
    val rank = if (isFlipped) rankIdx else 7 - rankIdx
    return Square.entries[rank * 8 + file]
}