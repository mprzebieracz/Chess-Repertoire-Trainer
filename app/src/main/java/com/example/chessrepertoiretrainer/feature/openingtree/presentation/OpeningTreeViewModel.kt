package com.example.chessrepertoiretrainer.feature.openingtree.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.chessrepertoiretrainer.core.chess.controller.BoardAnnotations
import com.example.chessrepertoiretrainer.core.chess.controller.DefaultChessBoardController
import com.example.chessrepertoiretrainer.core.chess.domain.Arrow
import com.example.chessrepertoiretrainer.core.chess.domain.findLegalMoveBySan
import com.example.chessrepertoiretrainer.feature.openingtree.data.OpeningTree
import com.example.chessrepertoiretrainer.feature.openingtree.data.OpeningTreeMoveAggregate
import com.example.chessrepertoiretrainer.feature.openingtree.data.OpeningTreeNode
import com.github.bhlangonijr.chesslib.Side
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class OpeningTreeMoveUi(
    val moveSan: String,
    val toFen: String,
    val games: Long,
    val winPercent: Int,
    val drawPercent: Int,
    val lossPercent: Int
)

data class OpeningTreeUiState(
    val isLoading: Boolean = true,
    val statusMessage: String? = null,
    val currentFen: String? = null,
    val pathMoves: List<String> = emptyList(),
    val moves: List<OpeningTreeMoveUi> = emptyList(),
    val canGoBack: Boolean = false
)

class OpeningTreeViewModel(private val tree: OpeningTree?) : ViewModel() {

    val chessController = DefaultChessBoardController()
    val annotations = BoardAnnotations()

    private val _uiState = MutableStateFlow(OpeningTreeUiState())
    val uiState: StateFlow<OpeningTreeUiState> = _uiState.asStateFlow()

    init {
        chessController.onMoveApplied = { applied ->
            val t = tree
            if (t != null && _uiState.value.currentFen != applied.fenAfter) {
                applyFen(applied.fenAfter, t, updateBoard = false)
            }
        }

        if (tree != null) {
            chessController.orientForSide(if (tree.playerIsBlack) Side.BLACK else Side.WHITE)
            applyFen(tree.rootFen, tree, updateBoard = true)
        } else {
            showEmptyState("Opening tree not found. Please go back and search again.")
        }
    }

    fun onMoveSelected(moveSan: String) {
        val board = chessController.getBoard()
        val move = board.findLegalMoveBySan(moveSan)
        if (move != null) chessController.onMove(move)
        else _uiState.value =
            _uiState.value.copy(statusMessage = "Selected move $moveSan is invalid on the board.")
    }

    fun onGoBack() {
        val t = tree ?: return
        val currentFen = _uiState.value.currentFen ?: return
        val parentFen = t.getNode(currentFen)?.parentFen ?: return
        applyFen(parentFen, t, updateBoard = true)
    }

    fun onGoRoot() {
        val t = tree ?: return
        applyFen(t.rootFen, t, updateBoard = true)
    }

    private fun applyFen(fen: String, tree: OpeningTree, updateBoard: Boolean) {
        val node = tree.getNode(fen)
        if (node == null) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                statusMessage = null,
                currentFen = fen,
                pathMoves = emptyList(),
                moves = emptyList(),
                canGoBack = fen != tree.rootFen
            )
            annotations.arrows = emptyList()
            if (updateBoard) chessController.loadPositionFromFen(fen)
            return
        }

        val path = buildPathForNode(node, tree)
        val movesUi =
            node.children.values.asSequence().sortedByDescending { it.games }.map(::toMoveUi)
                .toList()

        _uiState.value = _uiState.value.copy(
            isLoading = false,
            statusMessage = null,
            currentFen = fen,
            pathMoves = path,
            moves = movesUi,
            canGoBack = fen != tree.rootFen
        )

        if (updateBoard) chessController.loadPositionFromFen(fen)

        val totalGames = movesUi.sumOf { it.games }.coerceAtLeast(1L)
        val board = chessController.getBoard()
        annotations.arrows = movesUi.mapNotNull { moveUi ->
            val move = board.findLegalMoveBySan(moveUi.moveSan) ?: return@mapNotNull null
            val pct = moveUi.games.toFloat() / totalGames.toFloat()
            val alpha = when {
                pct >= 0.50f -> 1.00f
                pct >= 0.25f -> 0.80f
                pct >= 0.10f -> 0.60f
                pct >= 0.03f -> 0.42f
                else -> 0.28f
            }
            Arrow(move.from, move.to, alpha)
        }
    }

    private fun buildPathForNode(node: OpeningTreeNode, tree: OpeningTree): List<String> {
        val path = mutableListOf<String>()
        var current: OpeningTreeNode? = node
        while (current != null && current.moveSanFromParent != null) {
            path.add(current.moveSanFromParent)
            val parentFen = current.parentFen
            current = if (parentFen != null) tree.getNode(parentFen) else null
        }
        return path.asReversed()
    }

    private fun toMoveUi(aggregate: OpeningTreeMoveAggregate): OpeningTreeMoveUi {
        val totalGames = aggregate.games.takeIf { it > 0L } ?: 1L
        return OpeningTreeMoveUi(
            moveSan = aggregate.moveSan,
            toFen = aggregate.toFen,
            games = aggregate.games,
            winPercent = ((aggregate.wins * 100L) / totalGames).toInt(),
            drawPercent = ((aggregate.draws * 100L) / totalGames).toInt(),
            lossPercent = ((aggregate.losses * 100L) / totalGames).toInt()
        )
    }

    private fun showEmptyState(message: String) {
        _uiState.value = OpeningTreeUiState(
            isLoading = false,
            statusMessage = message,
            currentFen = null,
            pathMoves = emptyList(),
            moves = emptyList(),
            canGoBack = false
        )
        chessController.resetBoard()
    }

    class Factory(private val tree: OpeningTree?) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            return OpeningTreeViewModel(tree) as T
        }
    }
}
