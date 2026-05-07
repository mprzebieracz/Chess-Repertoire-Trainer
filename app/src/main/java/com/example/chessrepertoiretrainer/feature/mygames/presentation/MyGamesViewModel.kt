package com.example.chessrepertoiretrainer.feature.mygames.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.chessrepertoiretrainer.core.database.entity.SavedGame
import com.example.chessrepertoiretrainer.feature.mygames.data.GameSyncManager
import com.example.chessrepertoiretrainer.feature.mygames.data.SavedGameRepository
import com.example.chessrepertoiretrainer.feature.mygames.domain.model.CATEGORIES
import com.example.chessrepertoiretrainer.feature.mygames.domain.model.GameFilter
import com.example.chessrepertoiretrainer.feature.mygames.domain.model.GameStats
import com.example.chessrepertoiretrainer.feature.mygames.domain.model.StatsTimeRange
import com.example.chessrepertoiretrainer.feature.mygames.domain.model.sinceEpochMs
import com.example.chessrepertoiretrainer.feature.mygames.domain.model.toGameStats
import com.example.chessrepertoiretrainer.feature.settings.data.UserSettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MyGamesUiState(
    val isSyncing: Boolean = false,
    val syncProgress: String? = null,
    val syncError: String? = null,
    val gameCounts: Map<String, Int> = emptyMap(),
    val selectedTimeRange: StatsTimeRange = StatsTimeRange.DAYS_30,
    val statsRows: List<Pair<String, GameStats>> = emptyList(),
    val activeFilter: GameFilter = GameFilter()
)

@OptIn(ExperimentalCoroutinesApi::class)
class MyGamesViewModel(
    private val repository: SavedGameRepository,
    private val syncManager: GameSyncManager,
    private val settingsRepository: UserSettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyGamesUiState())
    val uiState: StateFlow<MyGamesUiState> = _uiState.asStateFlow()

    val games: StateFlow<List<SavedGame>> = _uiState
        .flatMapLatest { state ->
            settingsRepository.settingsFlow.flatMapLatest { settings ->
                val username = if (state.activeFilter.platform == "chess.com") {
                    settings.chessComUsername
                }
                else if (state.activeFilter.platform == "lichess") {
                    settings.lichessUsername
                }
                else {
                    settings.lichessUsername.ifBlank { settings.chessComUsername }
                }
                repository.getGamesFiltered(
                    username = username,
                    platform = state.activeFilter.platform,
                    category = state.activeFilter.timeCategory,
                    result = state.activeFilter.playerResult,
                    isWhite = state.activeFilter.isPlayerWhite
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            refreshCounts()
            refreshStats()
        }
    }

    fun sync() {
        if (_uiState.value.isSyncing) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSyncing = true, syncProgress = "Starting sync…", syncError = null
            )
            try {
                syncManager.syncAll { platform, count ->
                    _uiState.value = _uiState.value.copy(
                        syncProgress = "${platform.replaceFirstChar { it.uppercase() }}: $count fetched…"
                    )
                }
                refreshCounts()
                refreshStats()
                _uiState.value = _uiState.value.copy(
                    isSyncing = false, syncProgress = null
                )
            }
            catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    syncProgress = null,
                    syncError = e.message ?: "Sync failed"
                )
            }
        }
    }

    fun setTimeRange(range: StatsTimeRange) {
        _uiState.value = _uiState.value.copy(selectedTimeRange = range)
        viewModelScope.launch { refreshStats() }
    }

    fun setFilter(filter: GameFilter) {
        _uiState.value = _uiState.value.copy(activeFilter = filter)
    }

    private suspend fun refreshCounts() {
        val settings = settingsRepository.settingsFlow.map { it }.stateIn(viewModelScope).value
        val counts = mutableMapOf<String, Int>()
        if (settings.lichessUsername.isNotBlank()) {
            counts["lichess"] = repository.countGames("lichess", settings.lichessUsername)
        }
        if (settings.chessComUsername.isNotBlank()) {
            counts["chess.com"] = repository.countGames("chess.com", settings.chessComUsername)
        }
        _uiState.value = _uiState.value.copy(gameCounts = counts)
    }

    private suspend fun refreshStats() {
        val settings = settingsRepository.settingsFlow.map { it }.stateIn(viewModelScope).value
        val username = settings.lichessUsername.ifBlank { settings.chessComUsername }
        if (username.isBlank()) return

        val since = _uiState.value.selectedTimeRange.sinceEpochMs()
        val rows = CATEGORIES.map { category ->
            val raw = repository.getStats(
                username = username,
                platform = null,
                category = if (category == "all") null else category,
                since = since
            )
            category.replaceFirstChar { it.uppercase() } to raw.toGameStats()
        }
        _uiState.value = _uiState.value.copy(statsRows = rows)
    }

    class Factory(
        private val repository: SavedGameRepository,
        private val syncManager: GameSyncManager,
        private val settingsRepository: UserSettingsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            return MyGamesViewModel(repository, syncManager, settingsRepository) as T
        }
    }
}
