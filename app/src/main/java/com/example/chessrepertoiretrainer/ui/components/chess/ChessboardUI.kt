package com.example.chessrepertoiretrainer.ui.components.chess

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.chessrepertoiretrainer.data.BoardTheme
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.Side
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.move.Move
import kotlin.math.roundToInt

// --- Colors ---

data class BoardThemeColors(
    val lightSquare: Color,
    val darkSquare: Color
)

val ClassicBoardThemeColors = BoardThemeColors(
    lightSquare = Color(0xFFEBECD0),
    darkSquare = Color(0xFF779556)
)

val BlueBoardThemeColors = BoardThemeColors(
    lightSquare = Color(0xFFE0E8FF),
    darkSquare = Color(0xFF4A6FEA)
)

val BrownBoardThemeColors = BoardThemeColors(
    lightSquare = Color(0xFFF0E0D0),
    darkSquare = Color(0xFFB58863)
)

val LocalBoardThemeColors = androidx.compose.runtime.staticCompositionLocalOf { ClassicBoardThemeColors }
private val SelectedSquareColor = Color(0xBBF5F682)
private val LastMoveHighlightColor = Color(0x88F5F682)
private val HoverHighlightColor = Color(0x66FFFFFF)

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
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .onGloballyPositioned { boardSizePx = it.size.width.toFloat() }
            .shadow(8.dp, RoundedCornerShape(4.dp))
            .clip(RoundedCornerShape(4.dp))
            .border(2.dp, Color(0xFF312E2B), RoundedCornerShape(4.dp))
    ) {
        val squareSizePx = if (boardSizePx > 0) boardSizePx / 8 else 0f

        // 1. Draw the Grid
        Column(modifier = Modifier.fillMaxSize()) {
            val ranks = if (state.isFlipped) 0..7 else 7 downTo 0
            val files = if (state.isFlipped) 7 downTo 0 else 0..7

            for (rankIndex in ranks) {
                Row(modifier = Modifier.weight(1f)) {
                    for (fileIndex in files) {
                        val square = Square.entries[rankIndex * 8 + fileIndex]

                        ChessSquare(
                            square = square,
                            piece = board.getPiece(square),
                            isDark = (rankIndex + fileIndex) % 2 == 0,
                            isSelected = state.selectedSquare == square,
                            isLastMove = state.lastMove?.from == square || state.lastMove?.to == square,
                            isLegalMove = legalMoves.contains(square),
                            isHovered = state.hoveredSquare == square,
                            isHiddenForDrag = draggingSquare == square,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .setupDragGestures(
                                    square = square,
                                    piece = board.getPiece(square),
                                    sideToMove = board.sideToMove,
                                    squareSizePx = squareSizePx,
                                    isFlipped = state.isFlipped,
                                    rankIndex = rankIndex,
                                    fileIndex = fileIndex,
                                    state = state,
                                    onDragStart = { sq, offset ->
                                        draggingSquare = sq
                                        dragOffset = Offset.Zero
                                        initialTouchOffset = offset
                                    },
                                    onDragUpdate = { offset -> dragOffset += offset },
                                    onDragEnd = { draggingSquare = null; dragOffset = Offset.Zero }
                                )
                        )
                    }
                }
            }
        }

        // 2. Draw the Floating Piece (if dragging)
        if (draggingSquare != null && squareSizePx > 0f) {
            DraggedPiece(
                piece = board.getPiece(draggingSquare!!),
                square = draggingSquare!!,
                squareSizePx = squareSizePx,
                isFlipped = state.isFlipped,
                initialTouchOffset = initialTouchOffset,
                dragOffset = dragOffset
            )
        }

        val promotion = state.pendingPromotion
        if (promotion != null) {
            val promoPiece = board.getPiece(promotion.from)
            val side = promoPiece.pieceSide
            val options = if (side == Side.WHITE) {
                listOf(
                    Piece.WHITE_QUEEN,
                    Piece.WHITE_ROOK,
                    Piece.WHITE_BISHOP,
                    Piece.WHITE_KNIGHT
                )
            } else {
                listOf(
                    Piece.BLACK_QUEEN,
                    Piece.BLACK_ROOK,
                    Piece.BLACK_BISHOP,
                    Piece.BLACK_KNIGHT
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x88000000)),
                contentAlignment = Alignment.Center
            ) {
                Row {
                    options.forEach { promo ->
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clickable { state.promotePendingMove(promo) },
                            contentAlignment = Alignment.Center
                        ) {
                            PieceDisplay(promo, modifier = Modifier.fillMaxSize())
                        }
                    }
                }
            }
        }
    }
}

// --- Sub-Components ---

@Composable
private fun ChessSquare(
    square: Square,
    piece: Piece,
    isDark: Boolean,
    isSelected: Boolean,
    isLastMove: Boolean,
    isLegalMove: Boolean,
    isHovered: Boolean,
    isHiddenForDrag: Boolean,
    modifier: Modifier = Modifier
) {
    val boardColors = LocalBoardThemeColors.current

    Box(
        modifier = modifier.background(if (isDark) boardColors.darkSquare else boardColors.lightSquare),
        contentAlignment = Alignment.Center
    ) {
        if (isLastMove) Box(
            modifier = Modifier
                .fillMaxSize()
                .background(LastMoveHighlightColor)
        )
        if (isSelected) Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SelectedSquareColor)
        )
        if (isHovered) Box(
            modifier = Modifier
                .fillMaxSize()
                .background(HoverHighlightColor)
        )

        if (isLegalMove) {
            if (piece != Piece.NONE) {
                Box(
                    modifier = Modifier
                        .fillMaxSize(0.9f)
                        .border(4.dp, Color(0x40000000), CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(Color(0x40000000), CircleShape)
                )
            }
        }

        if (piece != Piece.NONE && !isHiddenForDrag) {
            PieceDisplay(piece)
        }
    }
}

@Composable
private fun DraggedPiece(
    piece: Piece,
    square: Square,
    squareSizePx: Float,
    isFlipped: Boolean,
    initialTouchOffset: Offset,
    dragOffset: Offset
) {
    val density = LocalDensity.current
    val visualFile = if (isFlipped) 7 - square.file.ordinal else square.file.ordinal
    val visualRank = if (isFlipped) square.rank.ordinal else 7 - square.rank.ordinal

    val startXPx = visualFile * squareSizePx
    val startYPx = visualRank * squareSizePx
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

// --- Drag Gesture Extension ---

private fun Modifier.setupDragGestures(
    square: Square,
    piece: Piece,
    sideToMove: com.github.bhlangonijr.chesslib.Side,
    squareSizePx: Float,
    isFlipped: Boolean,
    rankIndex: Int,
    fileIndex: Int,
    state: ChessBoardController,
    onDragStart: (Square, Offset) -> Unit,
    onDragUpdate: (Offset) -> Unit,
    onDragEnd: () -> Unit
): Modifier = this
    .clickable { state.onSquareClick(square) }
    .pointerInput(isFlipped) {
        detectDragGestures(
            onDragStart = { offset ->
                if (piece != Piece.NONE && piece.pieceSide == sideToMove) {
                    onDragStart(square, offset)
                    state.onSquareClick(square)
                }
            },
            onDrag = { change, dragAmount ->
                change.consume()
                onDragUpdate(dragAmount)

                // Calculate hovered square
                val currentX =
                    (if (isFlipped) 7 - fileIndex else fileIndex) * squareSizePx + change.position.x
                val currentY =
                    (if (isFlipped) rankIndex else 7 - rankIndex) * squareSizePx + change.position.y

                val targetFile = (currentX / squareSizePx).toInt().coerceIn(0, 7)
                    .let { if (isFlipped) 7 - it else it }
                val targetRank = (currentY / squareSizePx).toInt().coerceIn(0, 7)
                    .let { if (isFlipped) it else 7 - it }

                state.hoveredSquare = Square.entries.toTypedArray()[targetRank * 8 + targetFile]
            },
            onDragEnd = {
                state.hoveredSquare?.let { target ->
                    if (target != square) state.onMove(Move(square, target))
                }
                state.hoveredSquare = null
                onDragEnd()
            },
            onDragCancel = {
                state.hoveredSquare = null
                onDragEnd()
            }
        )
    }

@Composable
fun PieceDisplay(piece: Piece, modifier: Modifier = Modifier) {
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
