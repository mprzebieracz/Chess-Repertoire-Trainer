package com.example.chessrepertoiretrainer.core.chess.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import com.example.chessrepertoiretrainer.core.chess.controller.ChessBoardController
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.Side
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.move.Move

@Composable
internal fun ChessboardGrid(
    state: ChessBoardController,
    board: Board,
    legalMoves: List<Square>,
    draggingSquare: Square?,
    squareSizePx: Float,
    onDragStart: (Square, Offset) -> Unit,
    onDragUpdate: (Offset) -> Unit,
    onDragEnd: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        val ranks = if (state.isFlipped) 0..<8 else 8 - 1 downTo 0
        val files = if (state.isFlipped) 8 - 1 downTo 0 else 0..<8

        for (rankIndex in ranks) {
            Row(modifier = Modifier.weight(1f)) {
                for (fileIndex in files) {
                    val square = squareAt(rankIndex, fileIndex)
                    val piece = board.getPiece(square)

                    ChessSquare(
                        piece = piece,
                        isDark = (rankIndex + fileIndex) % 2 == 0,
                        isSelected = state.selectedSquare == square,
                        isLastMove = state.lastMove?.from == square || state.lastMove?.to == square,
                        isLegalMove = legalMoves.contains(square),
                        isHovered = state.hoveredSquare == square,
                        isMarked = state.markedSquare == square,
                        isHiddenForDrag = draggingSquare == square,
                        modifier = Modifier.weight(1f).fillMaxHeight().setupDragGestures(
                                square = square,
                                piece = piece,
                                sideToMove = board.sideToMove,
                                squareSizePx = squareSizePx,
                                isFlipped = state.isFlipped,
                                rankIndex = rankIndex,
                                fileIndex = fileIndex,
                                state = state,
                                onDragStart = onDragStart,
                                onDragUpdate = onDragUpdate,
                                onDragEnd = onDragEnd
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun ChessSquare(
    piece: Piece,
    isDark: Boolean,
    isSelected: Boolean,
    isLastMove: Boolean,
    isLegalMove: Boolean,
    isHovered: Boolean,
    isMarked: Boolean,
    isHiddenForDrag: Boolean,
    modifier: Modifier = Modifier
) {
    val boardColors = LocalBoardThemeColors.current

    Box(
        modifier = modifier.background(if (isDark) boardColors.darkSquare else boardColors.lightSquare), contentAlignment = Alignment.Center
    ) {
        if (isLastMove) {
            Box(
                modifier = Modifier.fillMaxSize().background(ChessUiConstants.BoardHighlights.lastMove)
            )
        }

        if (isSelected) {
            Box(
                modifier = Modifier.fillMaxSize().background(ChessUiConstants.BoardHighlights.selectedSquare)
            )
        }

        if (isLegalMove) {
            if (piece != Piece.NONE) {
                Box(
                    modifier = Modifier.fillMaxSize(ChessUiConstants.BoardHighlights.legalMoveFillScale).border(
                            ChessUiConstants.BoardHighlights.legalMoveBorderWidth, ChessUiConstants.BoardHighlights.legalMove, CircleShape
                        )
                )
            }
            else {
                Box(
                    modifier = Modifier.size(ChessUiConstants.BoardHighlights.legalMoveDotSize)
                        .background(ChessUiConstants.BoardHighlights.legalMove, CircleShape)
                )
            }
        }

        if (isHovered) {
            Box(
                modifier = Modifier.fillMaxSize()
                    .border(ChessUiConstants.BoardHighlights.hoverBorderWidth, ChessUiConstants.BoardHighlights.hoverBorder)
            )
        }

        if (isMarked) {
            Box(
                modifier = Modifier.fillMaxSize(ChessUiConstants.BoardHighlights.markedSquareScale)
                    .border(ChessUiConstants.BoardHighlights.markedBorderWidth, ChessUiConstants.BoardHighlights.markedBorder, CircleShape)
            )
        }

        if (piece != Piece.NONE && !isHiddenForDrag) {
            PieceDisplay(piece)
        }
    }
}

internal fun Modifier.setupDragGestures(
    square: Square,
    piece: Piece,
    sideToMove: Side,
    squareSizePx: Float,
    isFlipped: Boolean,
    rankIndex: Int,
    fileIndex: Int,
    state: ChessBoardController,
    onDragStart: (Square, Offset) -> Unit,
    onDragUpdate: (Offset) -> Unit,
    onDragEnd: () -> Unit
): Modifier = this.clickable { state.onSquareClick(square) }.pointerInput(isFlipped, piece, sideToMove) {
        detectDragGestures(onDragStart = { offset ->
            if (piece != Piece.NONE && piece.pieceSide == sideToMove) {
                onDragStart(square, offset)
                state.onSquareClick(square)
            }
        }, onDrag = { change, dragAmount ->
            change.consume()
            onDragUpdate(dragAmount)

            val newHovered = hoveredSquareFromPointer(
                squareSizePx = squareSizePx,
                isFlipped = isFlipped,
                rankIndex = rankIndex,
                fileIndex = fileIndex,
                pointerX = change.position.x,
                pointerY = change.position.y
            )

            if (state.hoveredSquare != newHovered) {
                state.hoveredSquare = newHovered
            }
        }, onDragEnd = {
            state.hoveredSquare?.let { target ->
                if (target != square) {
                    state.onMove(Move(square, target))
                }
            }
            state.hoveredSquare = null
            onDragEnd()
        }, onDragCancel = {
            state.hoveredSquare = null
            onDragEnd()
        })
    }




