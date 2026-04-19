@file:Suppress("unused")

package com.example.chessrepertoiretrainer.ui.components.chess

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal object ChessUiConstants {
    object BoardFrame {
        val shape = RoundedCornerShape(4.dp)
        val shadowElevation = 8.dp
        val borderWidth = 2.dp
        val borderColor = Color(0xFF312E2B)
    }

    object BoardHighlights {
        val selectedSquare = Color(0xBBF5F682)
        val lastMove = Color(0x88F5F682)
        val legalMove = Color(0x40000000)
        val hoverBorder = Color(0x33000000)
        val hoverBorderWidth = 2.dp
        val markedBorder = Color(0xFFFFC107)
        val legalMoveDotSize = 16.dp
        val legalMoveBorderWidth = 4.dp
        val markedBorderWidth = 3.dp
        const val legalMoveFillScale = 0.9f
        const val markedSquareScale = 0.8f
    }

    object DragPreview {
        const val scale = 1.2f
    }

    object PromotionOverlay {
        val scrimColor = Color(0x88000000)
        val pieceSize = 56.dp
    }

    object ScreenChrome {
        object BoardContainer {
            val horizontalPadding = 16.dp
            val verticalPadding = 8.dp
        }

        object Header {
            val horizontalPadding = 16.dp
            val verticalPadding = 12.dp
        }

        object Pgn {
            val horizontalPadding = 16.dp
            val verticalPadding = 4.dp
            val height = 40.dp
            val cornerRadius = 8.dp
            val textHorizontalPadding = 12.dp
            val textVerticalPadding = 8.dp
            val textFontSize = 14.sp
        }

        object Navigation {
            val buttonSize = 40.dp
            val iconSize = 32.dp
            val spacerWidth = 48.dp
            val rowPaddingVertical = 4.dp
        }

        object Actions {
            val rowPadding = 16.dp
            val buttonSpacing = 12.dp
        }

        object Spacing {
            val bottomSpacerHeight = 80.dp
        }
    }
}

@Stable
internal fun Modifier.chessboardFrame(onSizeChanged: (Float) -> Unit = {}): Modifier =
    this
        .fillMaxWidth()
        .aspectRatio(1f)
        .onGloballyPositioned { onSizeChanged(it.size.width.toFloat()) }
        .shadow(ChessUiConstants.BoardFrame.shadowElevation, ChessUiConstants.BoardFrame.shape)
        .clip(ChessUiConstants.BoardFrame.shape)
        .border(
            width = ChessUiConstants.BoardFrame.borderWidth,
            color = ChessUiConstants.BoardFrame.borderColor,
            shape = ChessUiConstants.BoardFrame.shape
        )





