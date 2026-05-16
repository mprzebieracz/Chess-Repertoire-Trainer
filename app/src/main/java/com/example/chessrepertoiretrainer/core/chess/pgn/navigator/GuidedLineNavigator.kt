package com.example.chessrepertoiretrainer.core.chess.pgn.navigator

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.chessrepertoiretrainer.core.database.entity.LineMove
import com.github.bhlangonijr.chesslib.move.Move

/**
 * Navigator backed by a fixed, pre-loaded [LineMove] list.
 *
 * Navigation (goPrevious/goNext/goTo) is allowed freely.
 * [onUserMove] accepts only the exact next expected move; any other move is rejected.
 * The tree is immutable after construction.
 */
class GuidedLineNavigator(moves: List<LineMove>, val initialFen: String = STARTING_FEN) :
    GameNavigator {

    companion object {
        const val STARTING_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

        fun empty() = GuidedLineNavigator(emptyList())
    }

    private val nodeList: List<MoveNode> = buildNodeList(moves)
    private var currentIndex = -1

    override var tree: MoveTree by mutableStateOf(buildTree(nodeList))
        private set
    override var currentNodeId: MoveNodeId? by mutableStateOf(null)
        private set
    override var currentMoveIndex: Int by mutableIntStateOf(-1)
        private set

    override val isAtStart: Boolean get() = currentIndex < 0
    override val isAtEnd: Boolean
        get() = nodeList.isEmpty() || currentIndex >= nodeList.lastIndex

    override val currentComment: String?
        get() = nodeList.getOrNull(currentIndex)?.comment?.takeIf { it.isNotBlank() }

    override var onPositionChanged: ((fen: String, lastMove: Move?) -> Unit)? = null

    val isEmpty: Boolean get() = nodeList.isEmpty()

    override fun goPrevious(): Boolean {
        if (currentIndex < 0) return false
        currentIndex--
        currentMoveIndex = currentIndex
        val node = nodeList.getOrNull(currentIndex)
        currentNodeId = node?.id
        return true
    }

    fun currentFen(): String = nodeList.getOrNull(currentIndex)?.fenAfter ?: initialFen

    override fun goNext(): Boolean {
        if (currentIndex >= nodeList.lastIndex) return false
        currentIndex++
        currentMoveIndex = currentIndex
        currentNodeId = nodeList[currentIndex].id
        return true
    }

    override fun goTo(id: MoveNodeId): Boolean {
        val index = nodeList.indexOfFirst { it.id == id }
        if (index < 0) return false
        currentIndex = index
        currentMoveIndex = currentIndex
        val node = nodeList[currentIndex]
        currentNodeId = node.id
        onPositionChanged?.invoke(node.fenAfter, node.move)
        return true
    }

    override fun reset() {
        currentIndex = -1
        currentMoveIndex = -1
        currentNodeId = null
        // No board update — caller is responsible for resetting the board.
    }

    /** Returns the SAN of the next move without advancing the cursor, or null if at end. */
    fun peekNextSan(): String? = nodeList.getOrNull(currentIndex + 1)?.san

    /** Returns the [MoveNode] at the current position, or null if at root. */
    fun currentNode(): MoveNode? = nodeList.getOrNull(currentIndex)

    override fun onUserMove(applied: AppliedMove): UserMoveResult {
        val expected = nodeList.getOrNull(currentIndex + 1) ?: return UserMoveResult.Rejected
        if (expected.san != applied.san) return UserMoveResult.Rejected
        currentIndex++
        currentMoveIndex = currentIndex
        currentNodeId = expected.id
        return UserMoveResult.Accepted(expected.id)
    }

    fun reload(moves: List<LineMove>) = GuidedLineNavigator(moves, initialFen)

    private fun buildNodeList(moves: List<LineMove>): List<MoveNode> {
        val nodes = mutableListOf<MoveNode>()
        moves.forEachIndexed { i, lm ->
            val id = MoveNodeId(i)
            val parentId = if (i == 0) null else MoveNodeId(i - 1)
            nodes.add(
                MoveNode(
                    id = id,
                    move = null,  // not available from stored LineMove data
                    san = lm.moveSan,
                    fenBefore = if (i == 0) initialFen else moves[i - 1].fen,
                    fenAfter = lm.fen,
                    comment = lm.comment,
                    parentId = parentId,
                )
            )
        }
        return nodes
    }

    private fun buildTree(nodes: List<MoveNode>): MoveTree {
        val byParent = nodes.groupBy { it.parentId }
        val rebuilt = nodes.map { node ->
            node.copy(children = byParent[node.id]?.map { it.id } ?: emptyList())
        }
        val nodesMap = rebuilt.associateBy { it.id }
        val rootChildren = rebuilt.filter { it.parentId == null }.map { it.id }
        return MoveTree(rootChildren, nodesMap)
    }
}
