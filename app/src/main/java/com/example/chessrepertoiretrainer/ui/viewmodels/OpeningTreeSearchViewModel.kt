package com.example.chessrepertoiretrainer.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.chessrepertoiretrainer.data.FetchedGame
import com.example.chessrepertoiretrainer.data.GameFetcherRegistry
import com.example.chessrepertoiretrainer.data.GameForOpeningTree
import com.example.chessrepertoiretrainer.data.OpeningTreeBuilder
import com.example.chessrepertoiretrainer.data.OpeningTreeCache
import com.example.chessrepertoiretrainer.data.OpeningTreeCacheKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel used by the Opening Tree "search" screen. It fetches games for an
 * arbitrary username from the network only, builds an opening tree entirely in
 * memory, and stores it in [OpeningTreeCache] under a synthetic profile id so
 * that [OpeningTreeViewModel] can display it without persisting anything to
 * the local database.
 */
class OpeningTreeSearchViewModel(
    private val fetcherRegistry: GameFetcherRegistry
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    // Synthetic negative ids for in-memory sessions so they don't collide with
    // real PlayerProfile ids coming from the database.
    private var nextSessionId: Long = -1L

    private fun allocateSessionProfileId(): Long = nextSessionId--

    fun searchAndPrepareOpeningTree(
        username: String,
        platform: String,
        maxGamesForTree: Int?,
        color: String,
        timeControlFilter: String,
        onProfileReady: (Long) -> Unit
    ) {
        if (username.isBlank() || _uiState.value.isSyncing) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSyncing = true,
                errorMessage = null,
                lastSyncSummary = null,
                statusMessage = "Fetching games..."
            )

            val normalizedUsername = username.trim()
            val normalizedPlatform = platform.trim().lowercase()

            val fetcher = fetcherRegistry.getFetcher(normalizedPlatform)
            if (fetcher == null) {
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    errorMessage = "Unsupported platform: $platform",
                    statusMessage = null
                )
                return@launch
            }

            val fetchedGames: List<FetchedGame> = try {
                // Do not apply the max-games limit here; use it after applying
                // color/time-control filters so the limit reflects the final
                // games used for the tree.
                fetcher.fetchGamesForUser(
                    username = normalizedUsername,
                    since = null,
                    maxGames = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    errorMessage = e.message ?: "Unexpected error while fetching games",
                    statusMessage = null
                )
                return@launch
            }

            if (fetchedGames.isEmpty()) {
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    lastSyncSummary = "No games found for this user",
                    statusMessage = null
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(statusMessage = "Preparing games...")

            val normalizedColor = color.trim().lowercase().ifEmpty { "both" }
            val normalizedTimeControl = timeControlFilter.trim()

            val filteredGames = OpeningTreeFilterUtils.filterFetchedGames(
                games = fetchedGames,
                color = normalizedColor,
                timeControlFilter = normalizedTimeControl
            )
            if (filteredGames.isEmpty()) {
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    lastSyncSummary = "No games match current filters.",
                    statusMessage = null
                )
                return@launch
            }

            val limitedGames = maxGamesForTree?.let { limit ->
                _uiState.value = _uiState.value.copy(statusMessage = "Limiting to $limit games...")
                filteredGames.take(limit)
            } ?: filteredGames

            _uiState.value = _uiState.value.copy(statusMessage = "Building opening tree...")

            val tree = withContext(Dispatchers.Default) {
                if (limitedGames.isEmpty()) {
                    null
                } else {
                    val gamesForTree = limitedGames.map { fetched ->
                        GameForOpeningTree(
                            pgn = fetched.pgn,
                            isUserWhite = fetched.isUserWhite,
                            resultTag = fetched.result
                        )
                    }
                    OpeningTreeBuilder.buildTree(gamesForTree)
                }
            }

            if (tree == null) {
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    lastSyncSummary = null,
                    statusMessage = "Failed to build opening tree."
                )
                return@launch
            }

            val sessionProfileId = allocateSessionProfileId()

            // Store the tree in the in-memory cache so OpeningTreeViewModel can
            // reuse it without touching the database.
            val cacheKey = OpeningTreeCacheKey(
                profileId = sessionProfileId,
                color = normalizedColor,
                timeControlFilter = normalizedTimeControl,
                maxGamesForTree = maxGamesForTree
            )
            OpeningTreeCache.clearForProfile(sessionProfileId)
            OpeningTreeCache.put(cacheKey, tree)

            _uiState.value = _uiState.value.copy(
                isSyncing = false,
                lastSyncSummary = "Prepared opening tree from ${limitedGames.size} games",
                statusMessage = "Opening tree ready."
            )

            onProfileReady(sessionProfileId)
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    data class SearchUiState(
        val errorMessage: String? = null,
        val isSyncing: Boolean = false,
        val lastSyncSummary: String? = null,
        val statusMessage: String? = null
    )

    class Factory(
        private val fetcherRegistry: GameFetcherRegistry
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            return OpeningTreeSearchViewModel(fetcherRegistry) as T
        }
    }
}

