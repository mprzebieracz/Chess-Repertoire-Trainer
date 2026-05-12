package com.example.chessrepertoiretrainer.feature.openingtree.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.chessrepertoiretrainer.core.network.games.FetchedGame
import com.example.chessrepertoiretrainer.core.network.games.GameFetcherRegistry
import com.example.chessrepertoiretrainer.feature.openingtree.data.GameForOpeningTree
import com.example.chessrepertoiretrainer.feature.openingtree.data.OpeningTree
import com.example.chessrepertoiretrainer.feature.openingtree.data.OpeningTreeBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class SyncPhase { Idle, FetchingGames, BuildingTree }

data class SearchUiState(
    val errorMessage: String? = null,
    val isSyncing: Boolean = false,
    val syncPhase: SyncPhase = SyncPhase.Idle,
    val fetchedGameCount: Int = 0,
    val lastSyncSummary: String? = null
)

class OpeningTreeSearchViewModel(private val fetcherRegistry: GameFetcherRegistry) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    fun searchAndPrepareOpeningTree(
        username: String,
        platform: String,
        maxGames: Int?,
        color: String,
        timeControlFilter: String,
        onTreeReady: (OpeningTree) -> Unit
    ) {
        if (username.isBlank() || _uiState.value.isSyncing) return

        val fetcher = fetcherRegistry.getFetcher(platform)
        if (fetcher == null) {
            _uiState.value = _uiState.value.copy(errorMessage = "Unsupported platform: $platform")
            return
        }

        viewModelScope.launch {
            _uiState.value = SearchUiState(isSyncing = true, syncPhase = SyncPhase.FetchingGames)

            val games: List<FetchedGame> = try {
                fetcher.fetchGamesForUser(
                    username = username.trim(),
                    maxGames = maxGames,
                    colorFilter = color,
                    timeControlFilter = timeControlFilter,
                    onProgress = { fetched ->
                        _uiState.value =
                            _uiState.value.copy(fetchedGameCount = fetched)
                    })
            } catch (e: Exception) {
                _uiState.value = SearchUiState(
                    errorMessage = e.message
                        ?: "Unexpected error while fetching games"
                )
                return@launch
            }

            if (games.isEmpty()) {
                _uiState.value = SearchUiState(lastSyncSummary = "No games found matching filters.")
                return@launch
            }

            _uiState.value = _uiState.value.copy(syncPhase = SyncPhase.BuildingTree)

            val tree = withContext(Dispatchers.Default) {
                OpeningTreeBuilder.buildTree(games = games.map {
                    GameForOpeningTree(
                        pgn = it.pgn,
                        isUserWhite = it.isUserWhite,
                        resultTag = it.result
                    )
                }, playerIsBlack = color == "black")
            }

            if (tree == null) {
                _uiState.value =
                    SearchUiState(errorMessage = "Failed to build opening tree from fetched games.")
                return@launch
            }

            _uiState.value =
                SearchUiState(lastSyncSummary = "Prepared opening tree from ${games.size} games")

            onTreeReady(tree)
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    class Factory(private val fetcherRegistry: GameFetcherRegistry) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            return OpeningTreeSearchViewModel(fetcherRegistry) as T
        }
    }
}