package com.example.chessrepertoiretrainer.ui.components.chess

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.chessrepertoiretrainer.R
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.move.Move
import kotlin.math.roundToInt

private val LightSquareColor = Color(0xFFEBECD0)
private val DarkSquareColor = Color(0xFF779556)
private val SelectedSquareColor = Color(0xBBF5F682)
private val LastMoveHighlightColor = Color(0x88F5F682)
private val HoverHighlightColor = Color(0x66FFFFFF)

@Composable
fun ChessboardUI(state: ChessBoardState) {
    val board = state.getBoard()
    val selectedSquare = state.selectedSquare
    val lastMove = state.lastMove
    val hoveredSquare = state.hoveredSquare
    val isFlipped = state.isFlipped
    val density = LocalDensity.current
    var boardSizePx by remember { mutableStateOf(0f) }
    
    var draggingSquare by remember { mutableStateOf<Square?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var initialTouchOffset by remember { mutableStateOf(Offset.Zero) }

    // Calculate legal moves for dots
    val legalMovesFromSelected = remember(state.boardState, selectedSquare) {
        if (selectedSquare != null) {
            board.legalMoves().filter { it.from == selectedSquare }.map { it.to }
        } else emptyList()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .onGloballyPositioned { boardSizePx = it.size.width.toFloat() }
            .shadow(8.dp, RoundedCornerShape(4.dp))
            .clip(RoundedCornerShape(4.dp))
            .border(2.dp, Color(0xFF312E2B), RoundedCornerShape(4.dp))
    ) {
        val squareSizePx = boardSizePx / 8

        Column(modifier = Modifier.fillMaxSize()) {
            val ranks = if (isFlipped) 0..7 else 7 downTo 0
            val files = if (isFlipped) 7 downTo 0 else 0..7

            for (rankIndex in ranks) {
                Row(modifier = Modifier.weight(1f)) {
                    for (fileIndex in files) {
                        val square = Square.values()[rankIndex * 8 + fileIndex]
                        val isDark = (rankIndex + fileIndex) % 2 == 0
                        val squareColor = if (isDark) DarkSquareColor else LightSquareColor
                        
                        // Highlight logic
                        val isSelected = selectedSquare == square
                        val isLastMoveOrigin = lastMove?.from == square
                        val isLastMoveDest = lastMove?.to == square
                        val isLegalMoveDot = legalMovesFromSelected.contains(square)
                        val isHovered = hoveredSquare == square

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(squareColor)
                                .pointerInput(isFlipped) {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            val piece = board.getPiece(square)
                                            if (piece != Piece.NONE && piece.pieceSide == board.sideToMove) {
                                                draggingSquare = square
                                                dragOffset = Offset.Zero
                                                initialTouchOffset = offset
                                                state.onSquareClick(square)
                                            }
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            dragOffset += dragAmount
                                            
                                            if (draggingSquare != null) {
                                                val visualFile = if (isFlipped) 7 - fileIndex else fileIndex
                                                val visualRank = if (isFlipped) rankIndex else 7 - rankIndex
                                                
                                                val currentX = visualFile * squareSizePx + initialTouchOffset.x + dragOffset.x
                                                val currentY = visualRank * squareSizePx + initialTouchOffset.y + dragOffset.y
                                                
                                                val targetVisualFile = (currentX / squareSizePx).toInt().coerceIn(0, 7)
                                                val targetVisualRank = (currentY / squareSizePx).toInt().coerceIn(0, 7)
                                                
                                                val targetFile = if (isFlipped) 7 - targetVisualFile else targetVisualFile
                                                val targetRank = if (isFlipped) targetVisualRank else 7 - targetVisualRank
                                                state.hoveredSquare = Square.values()[targetRank * 8 + targetFile]
                                            }
                                        },
                                        onDragEnd = {
                                            if (draggingSquare != null) {
                                                val visualFile = if (isFlipped) 7 - fileIndex else fileIndex
                                                val visualRank = if (isFlipped) rankIndex else 7 - rankIndex

                                                val currentX = visualFile * squareSizePx + initialTouchOffset.x + dragOffset.x
                                                val currentY = visualRank * squareSizePx + initialTouchOffset.y + dragOffset.y
                                                
                                                val targetVisualFile = (currentX / squareSizePx).toInt().coerceIn(0, 7)
                                                val targetVisualRank = (currentY / squareSizePx).toInt().coerceIn(0, 7)

                                                val targetFile = if (isFlipped) 7 - targetVisualFile else targetVisualFile
                                                val targetRank = if (isFlipped) targetVisualRank else 7 - targetVisualRank
                                                val targetSquare = Square.values()[targetRank * 8 + targetFile]
                                                
                                                if (targetSquare != draggingSquare) {
                                                    state.onMove(Move(draggingSquare!!, targetSquare))
                                                }
                                                draggingSquare = null
                                                dragOffset = Offset.Zero
                                                state.hoveredSquare = null
                                            }
                                        },
                                        onDragCancel = {
                                            draggingSquare = null
                                            dragOffset = Offset.Zero
                                            state.hoveredSquare = null
                                        }
                                    )
                                }
                                .clickable { state.onSquareClick(square) },
                            contentAlignment = Alignment.Center
                        ) {
                            // Last move highlights
                            if (isLastMoveOrigin || isLastMoveDest) {
                                Box(modifier = Modifier.fillMaxSize().background(LastMoveHighlightColor))
                            }
                            
                            // Selection highlight
                            if (isSelected) {
                                Box(modifier = Modifier.fillMaxSize().background(SelectedSquareColor))
                            }

                            // Hover highlight
                            if (isHovered) {
                                Box(modifier = Modifier.fillMaxSize().background(HoverHighlightColor))
                            }

                            // Legal move dot
                            if (isLegalMoveDot) {
                                val hasPiece = board.getPiece(square) != Piece.NONE
                                if (hasPiece) {
                                    // Circle around the piece for captures
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize(0.9f)
                                            .border(4.dp, Color(0x40000000), CircleShape)
                                    )
                                } else {
                                    // Simple dot for empty squares
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .background(Color(0x40000000), CircleShape)
                                    )
                                }
                            }

                            val piece = board.getPiece(square)
                            if (piece != Piece.NONE && draggingSquare != square) {
                                PieceDisplay(piece)
                            }
                        }
                    }
                }
            }
        }

        // Dragging piece overlay
        if (draggingSquare != null) {
            val piece = board.getPiece(draggingSquare!!)
            val fileIndex = draggingSquare!!.file.ordinal
            val rankIndex = draggingSquare!!.rank.ordinal
            
            val visualFile = if (isFlipped) 7 - fileIndex else fileIndex
            val visualRank = if (isFlipped) rankIndex else 7 - rankIndex

            val startXPx = visualFile * squareSizePx
            val startYPx = visualRank * squareSizePx

            // Set size to 1.2x of the square size
            val dragScale = 1.2f
            val sizeDp = with(density) { (squareSizePx * dragScale).toDp() }

            Box(
                modifier = Modifier
                    .size(sizeDp)
                    .offset {
                        IntOffset(
                            (startXPx + initialTouchOffset.x + dragOffset.x - (squareSizePx * dragScale / 2)).roundToInt(),
                            (startYPx + initialTouchOffset.y + dragOffset.y - (squareSizePx * dragScale / 2)).roundToInt()
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                 PieceDisplay(piece, modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
fun PieceDisplay(piece: Piece, modifier: Modifier = Modifier.fillMaxSize(0.85f)) {
    val drawableRes = getPieceDrawable(piece)
    if (drawableRes != 0) {
        Image(
            painter = painterResource(id = drawableRes),
            contentDescription = piece.name,
            modifier = modifier
        )
    }
}

private fun getPieceDrawable(piece: Piece): Int {
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
