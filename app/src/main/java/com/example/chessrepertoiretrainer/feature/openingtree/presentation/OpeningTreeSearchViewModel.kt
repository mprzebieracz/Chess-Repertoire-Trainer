package com.example.chessrepertoiretrainer.feature.openingtree.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.chessrepertoiretrainer.feature.openingtree.data.OnlineGamesFetchCoordinator
import com.example.chessrepertoiretrainer.feature.openingtree.data.OpeningTreePreparationCoordinator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel used by the Opening Tree "search" screen. It fetches games for an
 * arbitrary username from the network only, builds an opening tree entirely in
 * memory, and stores it in the opening-tree cache under a synthetic profile id so
 * that [OpeningTreeViewModel] can display it without persisting anything to
 * the local database.
 */
class OpeningTreeSearchViewModel(
    private val onlineGamesFetchCoordinator: OnlineGamesFetchCoordinator,
    private val openingTreePreparationCoordinator: OpeningTreePreparationCoordinator
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    // Synthetic negative ids for in-memory sessions so they don't collide with
    // real PlayerProfile ids coming from the database.
    private var nextSessionId: Long = -1L

    private fun allocateSessionProfileId(): Long = nextSessionId--

    fun searchAndPrepareOpeningTree(
        username: String, platform: String, maxGamesForTree: Int?, color: String, timeControlFilter: String, onProfileReady: (Long) -> Unit
    ) {
        if (username.isBlank() || _uiState.value.isSyncing) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSyncing = true, errorMessage = null, lastSyncSummary = null, statusMessage = "Fetching games..."
            )

            val fetchResult = onlineGamesFetchCoordinator.fetchGames(
                username = username, platform = platform
            )

            if (fetchResult.errorMessage != null) {
                _uiState.value = _uiState.value.copy(
                    isSyncing = false, errorMessage = fetchResult.errorMessage, statusMessage = null
                )
                return@launch
            }

            val fetchedGames = fetchResult.games

            if (fetchedGames.isEmpty()) {
                _uiState.value = _uiState.value.copy(
                    isSyncing = false, lastSyncSummary = "No games found for this user", statusMessage = null
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(statusMessage = "Preparing games...")

            val sessionProfileId = allocateSessionProfileId()
            val gamesUsedForTree = openingTreePreparationCoordinator.prepareFromFetchedGames(
                profileId = sessionProfileId,
                games = fetchedGames,
                color = color,
                timeControlFilter = timeControlFilter,
                maxGamesForTree = maxGamesForTree
            )

            if (gamesUsedForTree <= 0) {
                _uiState.value = _uiState.value.copy(
                    isSyncing = false, lastSyncSummary = "No games match current filters.", statusMessage = null
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(
                isSyncing = false, lastSyncSummary = "Prepared opening tree from $gamesUsedForTree games", statusMessage = "Opening tree ready."
            )

            onProfileReady(sessionProfileId)
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    data class SearchUiState(
        val errorMessage: String? = null, val isSyncing: Boolean = false, val lastSyncSummary: String? = null, val statusMessage: String? = null
    )

    class Factory(
        private val onlineGamesFetchCoordinator: OnlineGamesFetchCoordinator,
        private val openingTreePreparationCoordinator: OpeningTreePreparationCoordinator
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            return OpeningTreeSearchViewModel(
                onlineGamesFetchCoordinator = onlineGamesFetchCoordinator, openingTreePreparationCoordinator = openingTreePreparationCoordinator
            ) as T
        }
    }
}