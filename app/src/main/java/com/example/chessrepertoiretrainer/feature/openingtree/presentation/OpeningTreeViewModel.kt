package com.example.chessrepertoiretrainer.feature.openingtree.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.chessrepertoiretrainer.core.chess.controller.DefaultChessBoardController
import com.example.chessrepertoiretrainer.core.chess.domain.toSan
import com.example.chessrepertoiretrainer.feature.openingtree.data.OpeningTree
import com.example.chessrepertoiretrainer.feature.openingtree.data.OpeningTreeMoveAggregate
import com.example.chessrepertoiretrainer.feature.openingtree.data.OpeningTreeNode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class OpeningTreeMoveUi(val moveSan: String,
                             val toFen: String,
                             val games: Int,
                             val winPercent: Int,
                             val drawPercent: Int,
                             val lossPercent: Int)

data class OpeningTreeUiState(val isLoading: Boolean = true,
                              val statusMessage: String? = null,
                              val currentFen: String? = null,
                              val pathMoves: List<String> = emptyList(),
                              val moves: List<OpeningTreeMoveUi> = emptyList(),
                              val canGoBack: Boolean = false)

class OpeningTreeViewModel(private val tree: OpeningTree?) : ViewModel() {

    val chessController = DefaultChessBoardController()

    private val _uiState = MutableStateFlow(OpeningTreeUiState())
    val uiState: StateFlow<OpeningTreeUiState> = _uiState.asStateFlow()

    init {
        chessController.onMoveListener = { _, _, fen ->
            val t = tree
            if (t != null && _uiState.value.currentFen != fen) {
                applyFen(fen, t, updateBoard = false)
            }
        }

        if (tree != null) {
            if (tree.playerIsBlack && !chessController.isFlipped) {
                chessController.flipBoard()
            }
            applyFen(tree.rootFen, tree, updateBoard = true)
        }
        else {
            showEmptyState("Opening tree not found. Please go back and search again.")
        }
    }

    fun onMoveSelected(moveSan: String) {
        val board = chessController.getBoard()
        val move = board.legalMoves().firstOrNull { board.toSan(it) == moveSan }
        if (move != null) chessController.onMove(move)
        else _uiState.value =
            _uiState.value.copy(statusMessage = "Selected move $moveSan is invalid on the board.")
    }

    fun onGoBack() {
        val t = tree ?: return
        chessController.navigateBack()
        applyFen(chessController.boardState, t, updateBoard = false)
    }

    fun onGoRoot() {
        val t = tree ?: return
        applyFen(t.rootFen, t, updateBoard = true)
    }

    private fun applyFen(fen: String, tree: OpeningTree, updateBoard: Boolean) {
        val node = tree.getNode(fen)
        if (node == null) {
            _uiState.value = _uiState.value.copy(isLoading = false,
                                                 statusMessage = null,
                                                 currentFen = fen,
                                                 pathMoves = emptyList(),
                                                 moves = emptyList(),
                                                 canGoBack = fen != tree.rootFen)
            if (updateBoard) chessController.loadPositionFromFen(fen)
            return
        }

        val path = buildPathForNode(node, tree)
        val movesUi =
            node.children.values.asSequence().sortedByDescending { it.games }.map(::toMoveUi)
                .toList()

        _uiState.value = _uiState.value.copy(isLoading = false,
                                             statusMessage = null,
                                             currentFen = fen,
                                             pathMoves = path,
                                             moves = movesUi,
                                             canGoBack = fen != tree.rootFen)

        if (updateBoard) chessController.loadPositionFromFen(fen)
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
        val totalGames = aggregate.games.takeIf { it > 0 } ?: 1
        return OpeningTreeMoveUi(moveSan = aggregate.moveSan,
                                 toFen = aggregate.toFen,
                                 games = aggregate.games,
                                 winPercent = (aggregate.wins * 100) / totalGames,
                                 drawPercent = (aggregate.draws * 100) / totalGames,
                                 lossPercent = (aggregate.losses * 100) / totalGames)
    }

    private fun showEmptyState(message: String) {
        _uiState.value = OpeningTreeUiState(isLoading = false,
                                            statusMessage = message,
                                            currentFen = null,
                                            pathMoves = emptyList(),
                                            moves = emptyList(),
                                            canGoBack = false)
        chessController.resetBoard()
    }

    class Factory(private val tree: OpeningTree?) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            return OpeningTreeViewModel(tree) as T
        }
    }
}