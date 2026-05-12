package com.example.chessrepertoiretrainer.core.chess.ui

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