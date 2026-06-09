package com.example.chessrepertoiretrainer.core.chess.pgn.navigator

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.github.bhlangonijr.chesslib.move.Move

/**
 * Branching navigator: making a different move while not at the end creates a
 * variation (sibling branch) instead of trimming. If an identical move already
 * exists as a child, it is followed rather than duplicated.
 *
 * This is the navigator for the full-featured PGN analysis view.
 */
class TreeGameNavigator(val initialFen: String = STARTING_FEN) : GameNavigator {

    companion object {
        const val STARTING_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
    }

    private val nodesMap = mutableMapOf<MoveNodeId, MoveNode>()
    private val rootChildIds = mutableListOf<MoveNodeId>()
    private var nextIdValue = 0
    private var currentId: MoveNodeId? = null

    override var tree: MoveTree by mutableStateOf(MoveTree.EMPTY)
        private set
    override var currentNodeId: MoveNodeId? by mutableStateOf(null)
        private set
    override var currentMoveIndex: Int by mutableIntStateOf(-1)
        private set

    override val isAtStart: Boolean get() = currentId == null
    override val isAtEnd: Boolean
        get() {
            val id = currentId ?: return rootChildIds.isEmpty()
            return nodesMap[id]?.children.isNullOrEmpty()
        }

    override val currentComment: String?
        get() = currentId?.let { nodesMap[it]?.comment?.takeIf { c -> c.isNotBlank() } }

    override var onPositionChanged: ((fen: String, lastMove: Move?) -> Unit)? = null

    override fun goPrevious(): Boolean {
        val id = currentId ?: return false
        val node = nodesMap[id] ?: return false
        val parentId = node.parentId
        currentId = parentId
        currentNodeId = parentId
        updateMoveIndex()
        val targetFen = if (parentId == null) initialFen else nodesMap[parentId]!!.fenAfter
        val lastMove = parentId?.let { nodesMap[it]?.move }
        onPositionChanged?.invoke(targetFen, lastMove)
        return true
    }

    override fun goNext(): Boolean {
        val childrenIds =
            if (currentId == null) rootChildIds else nodesMap[currentId]?.children ?: return false
        val firstChild = childrenIds.firstOrNull() ?: return false
        val node = nodesMap[firstChild] ?: return false
        currentId = firstChild
        currentNodeId = firstChild
        updateMoveIndex()
        onPositionChanged?.invoke(node.fenAfter, node.move)
        return true
    }

    override fun goTo(id: MoveNodeId): Boolean {
        val node = nodesMap[id] ?: return false
        currentId = id
        currentNodeId = id
        updateMoveIndex()
        onPositionChanged?.invoke(node.fenAfter, node.move)
        return true
    }

    override fun reset() {
        currentId = null
        currentNodeId = null
        currentMoveIndex = -1
        onPositionChanged?.invoke(initialFen, null)
    }

    override fun onUserMove(applied: AppliedMove): UserMoveResult {
        val parentId = currentId
        val siblings = if (parentId == null) rootChildIds
        else nodesMap[parentId]?.children?.toMutableList() ?: mutableListOf()

        // Re-use existing child with same SAN if one exists.
        val existing = siblings.firstOrNull { nodesMap[it]?.san == applied.san }
        if (existing != null) {
            currentId = existing
            currentNodeId = existing
            updateMoveIndex()
            return UserMoveResult.Accepted(existing)
        }

        // Create a new node (variation if not at end of mainline).
        val newId = MoveNodeId(nextIdValue++)
        val newNode = MoveNode(
            id = newId,
            move = applied.move,
            san = applied.san,
            fenBefore = applied.fenBefore,
            fenAfter = applied.fenAfter,
            parentId = parentId,
        )
        nodesMap[newId] = newNode

        if (parentId == null) {
            rootChildIds.add(newId)
        }
        else {
            val parent = nodesMap[parentId]!!
            nodesMap[parentId] = parent.copy(children = parent.children + newId)
        }

        currentId = newId
        currentNodeId = newId
        updateMoveIndex()
        rebuildTree()
        return UserMoveResult.Accepted(newId)
    }

    private fun updateMoveIndex() {
        currentMoveIndex = computeDepth(currentId)
    }

    private fun computeDepth(id: MoveNodeId?): Int {
        var depth = -1
        var current = id
        while (current != null) {
            depth++
            current = nodesMap[current]?.parentId
        }
        return depth
    }

    private fun rebuildTree() {
        val nodesWithChildren = nodesMap.toMap()
        tree = MoveTree(rootChildIds.toList(), nodesWithChildren)
    }

    /** Clears all moves. The caller is responsible for also resetting the board position. */
    fun clear() {
        nodesMap.clear()
        rootChildIds.clear()
        nextIdValue = 0
        currentId = null
        currentNodeId = null
        currentMoveIndex = -1
        tree = MoveTree.EMPTY
    }
}