package com.example.chessrepertoiretrainer.feature.openingtree.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.chessrepertoiretrainer.core.chess.controller.DefaultChessBoardController
import com.example.chessrepertoiretrainer.feature.openingtree.data.OpeningTree
import com.example.chessrepertoiretrainer.feature.openingtree.data.OpeningTreeCache
import com.example.chessrepertoiretrainer.feature.openingtree.data.OpeningTreeCacheKey
import com.example.chessrepertoiretrainer.feature.openingtree.data.OpeningTreeMoveAggregate
import com.example.chessrepertoiretrainer.feature.openingtree.data.OpeningTreeNode
import com.example.chessrepertoiretrainer.feature.openingtree.domain.GamesRepository
import com.example.chessrepertoiretrainer.core.chess.domain.toSan
import com.example.chessrepertoiretrainer.feature.openingtree.data.OpeningTreePreparationCoordinator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OpeningTreeViewModel(
    private val profileId: Long,
    private val gamesRepository: GamesRepository,
    private val openingTreePreparationCoordinator: OpeningTreePreparationCoordinator,
    private val colorFilter: ColorFilter = ColorFilter.BOTH,
    private val timeControlFilter: String? = null,
    private val maxGamesForTree: Int? = null
) : ViewModel() {

    val chessController = DefaultChessBoardController()

    private var openingTree: OpeningTree? = null

    private val _uiState = MutableStateFlow(OpeningTreeUiState())
    val uiState: StateFlow<OpeningTreeUiState> = _uiState.asStateFlow()

    init {
        chessController.onFenChangedListener = { fen ->
            val tree = openingTree
            if (tree != null && _uiState.value.currentFen != fen) {
                applyFen(fen, tree, updateBoard = false)
            }
        }

        viewModelScope.launch {
            val cached = getCachedTreeForCurrentFilters()
            if (cached != null) {
                openingTree = cached
                updateBoardOrientation()
                applyFen(cached.rootFen, cached, updateBoard = true)
            }
            else {
                rebuildOpeningTree()
            }
        }
    }

    private fun getCachedTreeForCurrentFilters(): OpeningTree? {
        return OpeningTreeCache.get(currentCacheKey())
    }

    private suspend fun rebuildOpeningTree() {
        _uiState.value = OpeningTreeUiState(isLoading = true, statusMessage = "Gathering games...")

        val allGamesWithPgn = gamesRepository.getGamesWithPgnForProfile(profileId)

        if (allGamesWithPgn.isEmpty()) {
            showEmptyState("No games stored for this profile yet.")
            return
        }

        _uiState.value = _uiState.value.copy(statusMessage = "Building opening tree...")

        val gamesUsedForTree = openingTreePreparationCoordinator.prepareFromStoredGames(
            profileId = profileId,
            games = allGamesWithPgn,
            color = currentColorFilterValue(),
            timeControlFilter = currentTimeControlFilterValue(),
            maxGamesForTree = maxGamesForTree
        )

        val tree = getCachedTreeForCurrentFilters()
        if (tree == null || gamesUsedForTree <= 0) {
            showEmptyState("No games match current filters or the opening tree could not be built.")
            return
        }

        openingTree = tree
        updateBoardOrientation()
        applyFen(tree.rootFen, tree, updateBoard = true)
    }

    private fun applyFen(fen: String, tree: OpeningTree, updateBoard: Boolean) {
        val node = tree.getNode(fen)
        if (node == null) {
            // Position can be outside the opening tree; keep board interactive and show no moves.
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                statusMessage = null,
                currentFen = fen,
                pathMoves = emptyList(),
                moves = emptyList(),
                canGoBack = fen != tree.rootFen
            )
            if (updateBoard) {
                chessController.loadPositionFromFen(fen)
            }
            return
        }

        val path = buildPathForNode(node, tree)
        val movesUi = node.children.values
            .asSequence()
            .sortedByDescending { it.games }
            .map(::toMoveUi)
            .toList()

        val previous = _uiState.value
        _uiState.value = previous.copy(
            isLoading = false,
            statusMessage = null,
            currentFen = fen,
            pathMoves = path,
            moves = movesUi,
            canGoBack = fen != tree.rootFen
        )

        if (updateBoard) {
            chessController.loadPositionFromFen(fen)
        }
    }

    private fun buildPathForNode(
        node: OpeningTreeNode, tree: OpeningTree
    ): List<String> {
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
        return OpeningTreeMoveUi(
            moveSan = aggregate.moveSan,
            toFen = aggregate.toFen,
            games = aggregate.games,
            winPercent = (aggregate.wins * 100) / totalGames,
            drawPercent = (aggregate.draws * 100) / totalGames,
            lossPercent = (aggregate.losses * 100) / totalGames
        )
    }

    private fun showEmptyState(message: String) {
        openingTree = null
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

    private fun currentColorFilterValue(): String {
        return when (colorFilter) {
            ColorFilter.WHITE_ONLY -> "white"
            ColorFilter.BLACK_ONLY -> "black"
            ColorFilter.BOTH -> "both"
        }
    }

    private fun currentTimeControlFilterValue(): String {
        return timeControlFilter?.trim().orEmpty()
    }

    private fun currentCacheKey(): OpeningTreeCacheKey {
        return openingTreePreparationCoordinator.buildCacheKey(
            profileId = profileId,
            color = currentColorFilterValue(),
            timeControlFilter = currentTimeControlFilterValue(),
            maxGamesForTree = maxGamesForTree
        )
    }

    fun onMoveSelected(moveSan: String) {
        val board = chessController.getBoard()
        val move = board.legalMoves().firstOrNull { board.toSan(it) == moveSan }
        if (move != null) {
            chessController.onMove(move)
        } else {
            _uiState.value = _uiState.value.copy(
                statusMessage = "Selected move $moveSan is invalid on the board."
            )
        }
    }

    fun onGoBack() {
        chessController.navigateBack()
    }

    fun onGoRoot() {
        val tree = openingTree ?: return
        applyFen(tree.rootFen, tree, updateBoard = true)
    }

    data class OpeningTreeMoveUi(
        val moveSan: String, val toFen: String, val games: Int, val winPercent: Int, val drawPercent: Int, val lossPercent: Int
    )

    data class OpeningTreeUiState(
        val isLoading: Boolean = true,
        val statusMessage: String? = null,
        val currentFen: String? = null,
        val pathMoves: List<String> = emptyList(),
        val moves: List<OpeningTreeMoveUi> = emptyList(),
        val canGoBack: Boolean = false
    )

    private fun updateBoardOrientation() {
        val shouldBeFlipped = when (colorFilter) {
            ColorFilter.BLACK_ONLY -> true
            else -> false
        }

        if (chessController.isFlipped != shouldBeFlipped) {
            chessController.flipBoard()
        }
    }

    enum class ColorFilter { BOTH, WHITE_ONLY, BLACK_ONLY }


    class Factory(
        private val profileId: Long,
        private val gamesRepository: GamesRepository,
        private val openingTreePreparationCoordinator: OpeningTreePreparationCoordinator,
        private val colorFilter: ColorFilter,
        private val timeControlFilter: String?,
        private val maxGamesForTree: Int?
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            return OpeningTreeViewModel(
                profileId = profileId,
                gamesRepository = gamesRepository,
                openingTreePreparationCoordinator = openingTreePreparationCoordinator,
                colorFilter = colorFilter,
                timeControlFilter = timeControlFilter,
                maxGamesForTree = maxGamesForTree
            ) as T
        }
    }
}