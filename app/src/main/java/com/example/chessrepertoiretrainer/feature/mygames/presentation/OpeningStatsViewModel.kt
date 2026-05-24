package com.example.chessrepertoiretrainer.feature.mygames.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.chessrepertoiretrainer.core.opening.OpeningRegistry
import com.example.chessrepertoiretrainer.feature.mygames.data.SavedGameRepository
import com.example.chessrepertoiretrainer.feature.mygames.domain.model.OpeningFamilyGroup
import com.example.chessrepertoiretrainer.feature.mygames.domain.model.StatsTimeRange
import com.example.chessrepertoiretrainer.feature.mygames.domain.model.sinceEpochMs
import com.example.chessrepertoiretrainer.feature.mygames.domain.model.toOpeningStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OpeningStatsViewModel(
    val platform: String,
    val username: String,
    private val repository: SavedGameRepository,
    private val openingRegistry: OpeningRegistry
) : ViewModel() {

    data class UiState(
        val isLoading: Boolean = true,
        val groups: List<OpeningFamilyGroup> = emptyList(),
        val selectedTimeRange: StatsTimeRange = StatsTimeRange.ALL_TIME
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun setTimeRange(range: StatsTimeRange) {
        _uiState.update { it.copy(selectedTimeRange = range) }
        load()
    }

    fun toggleFamily(family: String) {
        _uiState.update { state ->
            state.copy(
                groups = state.groups.map { g ->
                    if (g.family == family) g.copy(isExpanded = !g.isExpanded) else g
                }
            )
        }
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val since = _uiState.value.selectedTimeRange.sinceEpochMs()
            val rawList = repository.getStatsByOpening(username, platform, since)

            val openingStatsList = rawList.map { raw ->
                val entry = openingRegistry.lookupByEco(raw.ecoCode)
                raw.toOpeningStats(
                    name = entry?.name ?: raw.ecoCode,
                    family = entry?.family ?: raw.ecoCode
                )
            }

            val groups = openingStatsList
                .groupBy { it.family }
                .map { (family, openings) ->
                    OpeningFamilyGroup(family = family, openings = openings.sortedByDescending { it.played })
                }
                .sortedByDescending { it.totalPlayed }

            _uiState.update { it.copy(isLoading = false, groups = groups) }
        }
    }

    class Factory(
        private val platform: String,
        private val username: String,
        private val repository: SavedGameRepository,
        private val openingRegistry: OpeningRegistry
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            OpeningStatsViewModel(platform, username, repository, openingRegistry) as T
    }
}
