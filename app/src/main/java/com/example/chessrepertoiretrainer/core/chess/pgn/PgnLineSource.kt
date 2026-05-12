package com.example.chessrepertoiretrainer.core.chess.pgn

interface PgnLineSource {
    val currentIndex: Int
    val isAtStart: Boolean
    val isAtEnd: Boolean
    val currentComment: String?

    fun moveAt(index: Int): LineMoveData?

    /** Advance to next move and return it, or null if already at end. */
    fun next(): LineMoveData?

    /** Retreat to previous move and return the new current move, or null if at start. */
    fun previous(): LineMoveData?

    fun reset()

    /** Returns variation moves at the current position. Default empty (single-line). */
    fun variationsAtCurrent(): List<LineMoveData> = emptyList()
}
