package com.example.chessrepertoiretrainer.ui.components.chess

import com.github.bhlangonijr.chesslib.Square

internal data class VisualSquarePosition(
    val file: Int, val rank: Int
)

internal fun visualPosition(square: Square, isFlipped: Boolean): VisualSquarePosition {
    val boardFile = square.file.ordinal
    val boardRank = square.rank.ordinal
    return VisualSquarePosition(
        file = boardFileToVisual(boardFile, isFlipped), rank = boardRankToVisual(boardRank, isFlipped)
    )
}

internal fun squareAt(rankIndex: Int, fileIndex: Int): Square = Square.entries[rankIndex * 8 + fileIndex]

internal fun hoveredSquareFromPointer(
    squareSizePx: Float, isFlipped: Boolean, rankIndex: Int, fileIndex: Int, pointerX: Float, pointerY: Float
): Square {
    val visualFileIndex = boardFileToVisual(fileIndex, isFlipped)
    val visualRankIndex = boardRankToVisual(rankIndex, isFlipped)

    val currentX = visualFileIndex * squareSizePx + pointerX
    val currentY = visualRankIndex * squareSizePx + pointerY

    val targetFile = pointerToBoardFile(currentX, squareSizePx, isFlipped)
    val targetRank = pointerToBoardRank(currentY, squareSizePx, isFlipped)

    return squareAt(targetRank, targetFile)
}

private fun boardFileToVisual(boardFile: Int, isFlipped: Boolean): Int = if (isFlipped) 8 - 1 - boardFile else boardFile

private fun boardRankToVisual(boardRank: Int, isFlipped: Boolean): Int = if (isFlipped) boardRank else 8 - 1 - boardRank

private fun pointerToBoardFile(currentX: Float, squareSizePx: Float, isFlipped: Boolean): Int {
    val visualFile = (currentX / squareSizePx).toInt().coerceIn(0, 8 - 1)
    return if (isFlipped) 8 - 1 - visualFile else visualFile
}

private fun pointerToBoardRank(currentY: Float, squareSizePx: Float, isFlipped: Boolean): Int {
    val visualRank = (currentY / squareSizePx).toInt().coerceIn(0, 8 - 1)
    return if (isFlipped) visualRank else 8 - 1 - visualRank
}
