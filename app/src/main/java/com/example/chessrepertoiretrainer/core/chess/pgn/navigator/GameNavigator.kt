package com.example.chessrepertoiretrainer.core.chess.pgn.navigator

import com.github.bhlangonijr.chesslib.move.Move

@JvmInline
value class MoveNodeId(val value: Int)

data class MoveNode(
    val id: MoveNodeId,
    val move: Move?,  // null for navigators built from stored SANs (no highlight in that case)
    val san: String,
    val fenBefore: String,
    val fenAfter: String,
    val comment: String? = null,
    val nags: List<Int> = emptyList(),
    val children: List<MoveNodeId> = emptyList(),
    val parentId: MoveNodeId? = null,
)

data class MoveTree(
    val rootChildren: List<MoveNodeId>,
    val nodes: Map<MoveNodeId, MoveNode>,
) {
    companion object {
        val EMPTY = MoveTree(emptyList(), emptyMap())
    }
}

data class AppliedMove(
    val move: Move,
    val san: String,
    val fenBefore: String,
    val fenAfter: String,
)

sealed interface UserMoveResult {
    data class Accepted(val nodeId: MoveNodeId) : UserMoveResult
    data object Rejected : UserMoveResult
}

interface GameNavigator {
    /** Full move tree, observable by Compose (backed by mutableStateOf in implementations). */
    val tree: MoveTree

    /** ID of the currently active node; null means root position (no moves played). */
    val currentNodeId: MoveNodeId?

    /** 0-based index in the mainline; -1 = root. Observable (backed by mutableStateOf). */
    val currentMoveIndex: Int

    val isAtStart: Boolean
    val isAtEnd: Boolean
    val currentComment: String?

    fun goPrevious(): Boolean
    fun goNext(): Boolean
    fun goTo(id: MoveNodeId): Boolean
    fun reset()

    /**
     * Called by the board controller when a user move is applied to the board.
     * The navigator decides whether to accept it (append / branch) or reject it.
     */
    fun onUserMove(applied: AppliedMove): UserMoveResult

    /**
     * Wired by the ViewModel: called when the navigator wants to change the board position
     * (e.g. after goPrevious/goNext/goTo/reset). Passes (newFen, lastMove or null).
     */
    var onPositionChanged: ((fen: String, lastMove: Move?) -> Unit)?
}
