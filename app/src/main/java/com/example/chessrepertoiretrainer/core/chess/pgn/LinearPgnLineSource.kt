package com.example.chessrepertoiretrainer.core.chess.pgn

import com.example.chessrepertoiretrainer.core.database.entity.LineMove

class LinearPgnLineSource(moves: List<LineMove>) : PgnLineSource {

    private val moves: List<LineMoveData> =
        moves.map { LineMoveData(it.moveSan, it.fen, it.comment) }

    override var currentIndex: Int = -1
        private set

    override val isAtStart: Boolean get() = currentIndex < 0
    override val isAtEnd: Boolean get() = moves.isEmpty() || currentIndex >= moves.lastIndex
    override val currentComment: String?
        get() = moves.getOrNull(currentIndex)?.comment?.takeIf { it.isNotBlank() }

    override fun moveAt(index: Int): LineMoveData? = moves.getOrNull(index)

    override fun next(): LineMoveData? {
        val nextIndex = currentIndex + 1
        if (nextIndex !in moves.indices) return null
        currentIndex = nextIndex
        return moves[currentIndex]
    }

    override fun previous(): LineMoveData? {
        if (currentIndex < 0) return null
        currentIndex--
        return moves.getOrNull(currentIndex)
    }

    override fun reset() {
        currentIndex = -1
    }

    val size: Int get() = moves.size
    val isEmpty: Boolean get() = moves.isEmpty()
}