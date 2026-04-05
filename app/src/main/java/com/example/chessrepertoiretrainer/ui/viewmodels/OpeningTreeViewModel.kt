package com.example.chessrepertoiretrainer.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.chessrepertoiretrainer.data.GameForOpeningTree
import com.example.chessrepertoiretrainer.data.OpeningTree
import com.example.chessrepertoiretrainer.data.OpeningTreeBuilder
import com.example.chessrepertoiretrainer.data.OpeningTreeCache
import com.example.chessrepertoiretrainer.data.OpeningTreeCacheKey
import com.example.chessrepertoiretrainer.domain.games.GamesRepository
import com.example.chessrepertoiretrainer.ui.components.chess.DefaultChessBoardController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OpeningTreeViewModel(
    private val profileId: Long,
    private val gamesRepository: GamesRepository,
    private val colorFilter: ColorFilter = ColorFilter.BOTH,
    private val timeControlFilter: String? = null,
    private val maxGamesForTree: Int? = null
) : ViewModel() {

    val chessController = DefaultChessBoardController()

    private var openingTree: OpeningTree? = null

    private val _uiState = MutableStateFlow(OpeningTreeUiState())
    val uiState: StateFlow<OpeningTreeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // First try to reuse a tree that was eagerly built and cached by
            // one of the preparation viewmodels (e.g. MyStatsViewModel,
            // OpeningTreeSearchViewModel, PlayerProfilesViewModel). If no
            // matching cached tree exists, fall back to building it on demand
            // as before.
            val cached = getCachedTreeForCurrentFilters()
            if (cached != null) {
                openingTree = cached
                updateBoardOrientation()
                applyFen(cached.rootFen, cached)
            } else {
                rebuildOpeningTree()
            }
        }
    }

    private fun getCachedTreeForCurrentFilters(): OpeningTree? {
        val normalizedColor = when (colorFilter) {
            ColorFilter.WHITE_ONLY -> "white"
            ColorFilter.BLACK_ONLY -> "black"
            ColorFilter.BOTH -> "both"
        }
        val normalizedTimeControl = timeControlFilter?.trim().orEmpty()

        val key = OpeningTreeCacheKey(
            profileId = profileId,
            color = normalizedColor,
            timeControlFilter = normalizedTimeControl,
            maxGamesForTree = maxGamesForTree
        )

        return OpeningTreeCache.get(key)
    }

    private suspend fun rebuildOpeningTree() {
        // Step 1: gather games from local database.
        _uiState.value = OpeningTreeUiState(
            isLoading = true,
            statusMessage = "Gathering games..."
        )

        val allGamesWithPgn = gamesRepository.getGamesWithPgnForProfile(profileId)

        if (allGamesWithPgn.isEmpty()) {
            openingTree = null
            _uiState.value = OpeningTreeUiState(
                isLoading = false,
                statusMessage = "No games stored for this profile yet.",
                currentFen = null,
                pathMoves = emptyList(),
                moves = emptyList()
            )
            chessController.resetBoard()
            return
        }

        // Step 2: apply color / time-control filters.
        _uiState.value = _uiState.value.copy(statusMessage = "Applying filters...")
        val filteredGames = OpeningTreeFilterUtils.filterGames(
            games = allGamesWithPgn,
            colorFilter = colorFilter,
            timeControlFilter = timeControlFilter
        )
        val gamesWithPgn = maxGamesForTree?.let { limit ->
            filteredGames.take(limit)
        } ?: filteredGames

        if (gamesWithPgn.isEmpty()) {
            openingTree = null
            _uiState.value = OpeningTreeUiState(
                isLoading = false,
                statusMessage = "No games match current filters.",
                currentFen = null,
                pathMoves = emptyList(),
                moves = emptyList()
            )
            chessController.resetBoard()
            return
        }

        // Step 3: build the opening tree from PGNs.
        _uiState.value = _uiState.value.copy(statusMessage = "Building opening tree...")

        val gamesForTree = gamesWithPgn.map { gwp ->
            GameForOpeningTree(
                pgn = gwp.pgn,
                isUserWhite = gwp.game.isUserWhite,
                resultTag = gwp.game.result
            )
        }

        openingTree = OpeningTreeBuilder.buildTree(gamesForTree)
        val tree = openingTree
        if (tree == null) {
            _uiState.value = OpeningTreeUiState(
                isLoading = false,
                statusMessage = "Failed to build opening tree.",
                currentFen = null,
                pathMoves = emptyList(),
                moves = emptyList()
            )
            chessController.resetBoard()
        } else {
            // Step 4: analyze statistics / prepare first position.
            _uiState.value = _uiState.value.copy(statusMessage = "Analyzing statistics...")
            updateBoardOrientation()
            applyFen(tree.rootFen, tree)
        }
    }

    private fun applyFen(fen: String, tree: OpeningTree) {
        val node = tree.getNode(fen) ?: run {
            _uiState.value = OpeningTreeUiState()
            chessController.resetBoard()
            return
        }

        val movesUi = node.children.values
            .sortedByDescending { it.games }
            .map { agg ->
                val total = if (agg.games <= 0) 1 else agg.games
                val winPercent = (agg.wins * 100) / total
                val drawPercent = (agg.draws * 100) / total
                val lossPercent = (agg.losses * 100) / total

                OpeningTreeMoveUi(
                    moveSan = agg.moveSan,
                    toFen = agg.toFen,
                    games = agg.games,
                    winPercent = winPercent,
                    drawPercent = drawPercent,
                    lossPercent = lossPercent
                )
            }

        val path = buildPathForNode(node, tree)

        val previous = _uiState.value
        _uiState.value = previous.copy(
            isLoading = false,
            statusMessage = null,
            currentFen = fen,
            pathMoves = path,
            moves = movesUi
        )

        chessController.loadPositionFromFen(fen)
    }

    private fun buildPathForNode(
        node: com.example.chessrepertoiretrainer.data.OpeningTreeNode,
        tree: OpeningTree
    ): List<String> {
        val path = mutableListOf<String>()
        var current: com.example.chessrepertoiretrainer.data.OpeningTreeNode? = node
        while (current != null && current.moveSanFromParent != null) {
            path.add(current.moveSanFromParent)
            val parentFen = current.parentFen
            current = if (parentFen != null) tree.getNode(parentFen) else null
        }
        return path.asReversed()
    }

    fun onMoveSelected(targetFen: String) {
        val tree = openingTree ?: return
        val node = tree.getNode(targetFen) ?: return
        applyFen(node.fen, tree)
    }

    fun onGoBack() {
        val tree = openingTree ?: return
        val currentFen = _uiState.value.currentFen ?: return
        val node = tree.getNode(currentFen) ?: return
        val parentFen = node.parentFen ?: return
        val parentNode = tree.getNode(parentFen) ?: return
        applyFen(parentNode.fen, tree)
    }

    fun onGoRoot() {
        val tree = openingTree ?: return
        applyFen(tree.rootFen, tree)
    }

    data class OpeningTreeMoveUi(
        val moveSan: String,
        val toFen: String,
        val games: Int,
        val winPercent: Int,
        val drawPercent: Int,
        val lossPercent: Int
    )

    data class OpeningTreeUiState(
        val isLoading: Boolean = true,
        val statusMessage: String? = null,
        val currentFen: String? = null,
        val pathMoves: List<String> = emptyList(),
        val moves: List<OpeningTreeMoveUi> = emptyList()
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
        private val colorFilter: ColorFilter,
        private val timeControlFilter: String?,
        private val maxGamesForTree: Int?
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            return OpeningTreeViewModel(
                profileId = profileId,
                gamesRepository = gamesRepository,
                colorFilter = colorFilter,
                timeControlFilter = timeControlFilter,
                maxGamesForTree = maxGamesForTree
            ) as T
        }
    }
}

/**
 * Returns true if a game with the given normalized [gameTimeCategory]
 * (e.g. "bullet", "blitz", "rapid", "classical") belongs to at least one
 * of the [selectedCategories]. When [selectedCategories] is empty, no
 * filtering is applied and the function always returns true.
 */
internal fun matchesTimeControlFilter(
    gameTimeCategory: String?,
    selectedCategories: Set<String>
): Boolean {
    if (selectedCategories.isEmpty()) return true

    val category = gameTimeCategory?.trim()?.lowercase() ?: return false
    return category in selectedCategories
}
