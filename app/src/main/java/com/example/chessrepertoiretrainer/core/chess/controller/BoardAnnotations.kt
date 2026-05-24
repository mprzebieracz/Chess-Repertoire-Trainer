package com.example.chessrepertoiretrainer.core.chess.controller

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.chessrepertoiretrainer.core.chess.domain.Arrow
import com.example.chessrepertoiretrainer.core.chess.domain.BoardMoveAnnotation

/**
 * View-only annotations rendered on top of the board. Owned by the
 * ViewModel alongside [ChessBoardController]; never read by board logic.
 */
class BoardAnnotations {
    var arrows: List<Arrow> by mutableStateOf(emptyList())
    var lastMoveAnnotation: BoardMoveAnnotation? by mutableStateOf(null)
}
