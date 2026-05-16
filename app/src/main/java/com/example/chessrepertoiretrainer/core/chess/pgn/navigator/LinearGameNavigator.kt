package com.example.chessrepertoiretrainer.core.chess.pgn.navigator

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.github.bhlangonijr.chesslib.move.Move

/**
 * Single-line navigator: new moves append at the end; making a different move
 * while in the middle trims all future moves first (no branching).
 *
 * This replicates the original DefaultChessBoardController history behaviour.
 */
class LinearGameNavigator(val initialFen: String = STARTING_FEN) : GameNavigator {

    companion object {
        const val STARTING_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
    }

    // Stored WITHOUT children — children are computed in rebuildTree().
    private val nodeList = mutableListOf<MoveNode>()
    private var nextIdValue = 0
    private var rawIndex = -1  // -1 = root position (not exposed directly)

    override var tree: MoveTree by mutableStateOf(MoveTree.EMPTY)
        private set
    override var currentNodeId: MoveNodeId? by mutableStateOf(null)
        private set
    override var currentMoveIndex: Int by mutableIntStateOf(-1)
        private set

    override val isAtStart: Boolean get() = rawIndex < 0
    override val isAtEnd: Boolean
        get() = nodeList.isEmpty() || rawIndex >= nodeList.lastIndex

    override val currentComment: String?
        get() = nodeList.getOrNull(rawIndex)?.comment?.takeIf { it.isNotBlank() }

    override var onPositionChanged: ((fen: String, lastMove: Move?) -> Unit)? = null

    override fun goPrevious(): Boolean {
        if (rawIndex < 0) return false
        rawIndex--
        currentMoveIndex = rawIndex
        val targetFen = if (rawIndex < 0) initialFen else nodeList[rawIndex].fenAfter
        val lastMove = nodeList.getOrNull(rawIndex)?.move
        currentNodeId = nodeList.getOrNull(rawIndex)?.id
        onPositionChanged?.invoke(targetFen, lastMove)
        return true
    }

    override fun goNext(): Boolean {
        if (rawIndex >= nodeList.lastIndex) return false
        rawIndex++
        currentMoveIndex = rawIndex
        val node = nodeList[rawIndex]
        currentNodeId = node.id
        onPositionChanged?.invoke(node.fenAfter, node.move)
        return true
    }

    override fun goTo(id: MoveNodeId): Boolean {
        val index = nodeList.indexOfFirst { it.id == id }
        if (index < 0) return false
        rawIndex = index
        currentMoveIndex = rawIndex
        val node = nodeList[rawIndex]
        currentNodeId = node.id
        onPositionChanged?.invoke(node.fenAfter, node.move)
        return true
    }

    override fun reset() {
        rawIndex = -1
        currentMoveIndex = -1
        currentNodeId = null
        onPositionChanged?.invoke(initialFen, null)
    }

    override fun onUserMove(applied: AppliedMove): UserMoveResult {
        // Trim any nodes beyond current position (future history discarded on divergence).
        while (nodeList.size > rawIndex + 1) {
            nodeList.removeAt(nodeList.lastIndex)
        }

        val newId = MoveNodeId(nextIdValue++)
        val parentId = nodeList.getOrNull(rawIndex)?.id
        val newNode = MoveNode(
            id = newId,
            move = applied.move,
            san = applied.san,
            fenBefore = applied.fenBefore,
            fenAfter = applied.fenAfter,
            parentId = parentId,
        )
        nodeList.add(newNode)
        rawIndex = nodeList.lastIndex
        currentMoveIndex = rawIndex
        currentNodeId = newId
        rebuildTree()
        return UserMoveResult.Accepted(newId)
    }

    private fun rebuildTree() {
        val byParent = nodeList.groupBy { it.parentId }
        val rebuilt = nodeList.map { node ->
            node.copy(children = byParent[node.id]?.map { it.id } ?: emptyList())
        }
        val nodesMap = rebuilt.associateBy { it.id }
        val rootChildren = rebuilt.filter { it.parentId == null }.map { it.id }
        tree = MoveTree(rootChildren, nodesMap)
    }

    /**
     * Clears all moves. The caller is responsible for also resetting the board position;
     * this method does not invoke [onPositionChanged].
     */
    fun clear() {
        nodeList.clear()
        nextIdValue = 0
        rawIndex = -1
        currentMoveIndex = -1
        currentNodeId = null
        tree = MoveTree.EMPTY
    }

    /** Flat SAN list in play order (for backwards-compat during migration). */
    val sanList: List<String> get() = nodeList.map { it.san }
}
