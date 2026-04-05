package com.example.chessrepertoiretrainer.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.chessrepertoiretrainer.data.GameForOpeningTree
import com.example.chessrepertoiretrainer.data.OpeningTree
import com.example.chessrepertoiretrainer.data.OpeningTreeBuilder
import com.example.chessrepertoiretrainer.domain.games.GamesRepository
import com.example.chessrepertoiretrainer.data.PlayerProfileRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel backing the "Your Games" screen. It lets the user enter a
 * username + platform, downloads games via the PlayerGamesRepository, and
 * exposes how many games are stored locally for that user.
 */
 class YourGamesViewModel(
    private val profileRepository: PlayerProfileRepository,
    private val gamesRepository: GamesRepository
 ) : ViewModel() {

    private val _uiState = MutableStateFlow(YourGamesUiState())
    val uiState: StateFlow<YourGamesUiState> = _uiState.asStateFlow()

    private val currentProfileId = MutableStateFlow<Long?>(null)

    private var openingTree: OpeningTree? = null

    private val _treeState = MutableStateFlow(OpeningTreeUiState())
    val treeState: StateFlow<OpeningTreeUiState> = _treeState.asStateFlow()

    /**
     * Reactive count of games stored locally for the currently selected
     * profile (if any).
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val gamesCount: StateFlow<Int> = currentProfileId
        .flatMapLatest { id ->
            if (id == null) {
                flowOf(emptyList())
            } else {
                gamesRepository.getGamesForProfile(id)
            }
        }
        .map { games -> games.size }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0
        )

    fun syncGamesForUser(username: String, platform: String, maxGames: Int? = 100) {
        if (username.isBlank()) return
        if (_uiState.value.isSyncing) return

        val normalizedUsername = username.trim()
        val normalizedPlatform = platform.trim().lowercase()

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSyncing = true,
                errorMessage = null,
                lastSyncSummary = null,
                lastProfileUsername = normalizedUsername,
                lastProfilePlatform = normalizedPlatform
            )

            try {
                val profile = profileRepository.getOrCreateProfile(
                    username = normalizedUsername,
                    platform = normalizedPlatform
                )
                currentProfileId.value = profile.id

                val result = gamesRepository.syncGamesForProfile(profile.id, maxGames)

                // After syncing, (re)build the opening tree from all stored games.
                rebuildOpeningTreeForProfile(profile.id)

                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    errorMessage = result.errorMessage,
                    lastSyncSummary = result.errorMessage?.let { null }
                        ?: "Downloaded ${result.newGames} new games"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    errorMessage = e.message ?: "Unexpected error during sync"
                )
            }
        }
    }

    /** Build or refresh the opening tree for the given profile from its stored games. */
    private suspend fun rebuildOpeningTreeForProfile(profileId: Long) {
        val gamesWithPgn = gamesRepository.getGamesWithPgnForProfile(profileId)
        if (gamesWithPgn.isEmpty()) {
            openingTree = null
            _treeState.value = OpeningTreeUiState()
            return
        }

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
            _treeState.value = OpeningTreeUiState()
        } else {
            _treeState.value = buildUiStateForFen(tree.rootFen, tree)
        }
    }

    private fun buildUiStateForFen(fen: String, tree: OpeningTree): OpeningTreeUiState {
        val node = tree.getNode(fen) ?: return OpeningTreeUiState()

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

        return OpeningTreeUiState(
            currentFen = fen,
            pathMoves = path,
            moves = movesUi
        )
    }

    private fun buildPathForNode(node: com.example.chessrepertoiretrainer.data.OpeningTreeNode, tree: OpeningTree): List<String> {
        val path = mutableListOf<String>()
        var current: com.example.chessrepertoiretrainer.data.OpeningTreeNode? = node
        while (current != null && current.moveSanFromParent != null) {
            path.add(current.moveSanFromParent)
            val parentFen = current.parentFen
            current = if (parentFen != null) tree.getNode(parentFen) else null
        }
        return path.asReversed()
    }

    fun onTreeMoveSelected(targetFen: String) {
        val tree = openingTree ?: return
        val node = tree.getNode(targetFen) ?: return
        _treeState.value = buildUiStateForFen(node.fen, tree)
    }

    fun onTreeGoBack() {
        val tree = openingTree ?: return
        val currentFen = _treeState.value.currentFen ?: return
        val node = tree.getNode(currentFen) ?: return
        val parentFen = node.parentFen ?: return
        val parentNode = tree.getNode(parentFen) ?: return
        _treeState.value = buildUiStateForFen(parentNode.fen, tree)
    }

    fun onTreeGoRoot() {
        val tree = openingTree ?: return
        _treeState.value = buildUiStateForFen(tree.rootFen, tree)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    data class YourGamesUiState(
        val errorMessage: String? = null,
        val isSyncing: Boolean = false,
        val lastSyncSummary: String? = null,
        val lastProfileUsername: String? = null,
        val lastProfilePlatform: String? = null
    )

    data class OpeningTreeMoveUi(
        val moveSan: String,
        val toFen: String,
        val games: Int,
        val winPercent: Int,
        val drawPercent: Int,
        val lossPercent: Int
    )

    data class OpeningTreeUiState(
        val currentFen: String? = null,
        val pathMoves: List<String> = emptyList(),
        val moves: List<OpeningTreeMoveUi> = emptyList()
    )

    class Factory(
        private val profileRepository: PlayerProfileRepository,
        private val gamesRepository: GamesRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            return YourGamesViewModel(profileRepository, gamesRepository) as T
        }
    }
}


