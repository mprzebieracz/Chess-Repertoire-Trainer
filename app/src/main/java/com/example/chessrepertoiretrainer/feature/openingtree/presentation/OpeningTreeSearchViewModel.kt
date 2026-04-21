package com.example.chessrepertoiretrainer.feature.openingtree.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.chessrepertoiretrainer.feature.openingtree.data.OpeningTreePreparationCoordinator
import com.example.chessrepertoiretrainer.feature.openingtree.data.fetcher.OnlineGamesFetchCoordinator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OpeningTreeSearchViewModel(
    private val onlineGamesFetchCoordinator: OnlineGamesFetchCoordinator,
    private val openingTreePreparationCoordinator: OpeningTreePreparationCoordinator
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    fun searchAndPrepareOpeningTree(
        username: String,
        platform: String,
        maxGamesForTree: Int?,
        color: String,
        timeControlFilter: String,
        onTreeReady: (username: String, platform: String) -> Unit
    ) {
        if (username.isBlank() || _uiState.value.isSyncing) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSyncing = true,
                errorMessage = null,
                lastSyncSummary = null,
                statusMessage = "Fetching games..."
            )

            val fetchResult = onlineGamesFetchCoordinator.fetchGames(
                username = username,
                platform = platform,
                maxGames = maxGamesForTree,
                colorFilter = color,
                timeControlFilter = timeControlFilter
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
                    isSyncing = false,
                    lastSyncSummary = "No games found matching filters.",
                    statusMessage = null
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(statusMessage = "Preparing opening tree...")

            val gamesUsedForTree = openingTreePreparationCoordinator.prepareFromFetchedGames(
                username = username,
                platform = platform,
                games = fetchedGames,
                color = color,
                timeControlFilter = timeControlFilter,
                maxGamesForTree = maxGamesForTree
            )

            _uiState.value = _uiState.value.copy(
                isSyncing = false,
                lastSyncSummary = "Prepared opening tree from $gamesUsedForTree games",
                statusMessage = "Opening tree ready."
            )

            onTreeReady(username, platform)
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
        private val onlineGamesFetchCoordinator: OnlineGamesFetchCoordinator,
        private val openingTreePreparationCoordinator: OpeningTreePreparationCoordinator
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            return OpeningTreeSearchViewModel(
                onlineGamesFetchCoordinator = onlineGamesFetchCoordinator,
                openingTreePreparationCoordinator = openingTreePreparationCoordinator
            ) as T
        }
    }
}