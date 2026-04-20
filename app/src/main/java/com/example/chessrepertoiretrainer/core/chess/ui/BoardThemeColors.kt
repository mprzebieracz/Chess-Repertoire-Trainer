package com.example.chessrepertoiretrainer.core.chess.ui

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

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

val LocalBoardThemeColors = staticCompositionLocalOf { ClassicBoardThemeColors }

