package com.example.chessrepertoiretrainer.feature.mygames.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.chessrepertoiretrainer.feature.mygames.data.SavedGameRepository
import com.example.chessrepertoiretrainer.feature.mygames.domain.model.CategoryStats
import com.example.chessrepertoiretrainer.feature.mygames.domain.model.STAT_CATEGORIES
import com.example.chessrepertoiretrainer.feature.mygames.domain.model.StatsTimeRange
import com.example.chessrepertoiretrainer.feature.mygames.domain.model.sinceEpochMs
import com.example.chessrepertoiretrainer.feature.mygames.domain.model.toGameStats
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AccountStatsUiState(
    val selectedTimeRange: StatsTimeRange = StatsTimeRange.DAYS_7,
    val totalGames: Int = 0,
    val categoryStats: List<CategoryStats> = emptyList(),
    val isLoading: Boolean = true
)

class AccountStatsViewModel(
    val platform: String,
    val username: String,
    private val repository: SavedGameRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountStatsUiState())
    val uiState: StateFlow<AccountStatsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { refresh() }
    }

    fun setTimeRange(range: StatsTimeRange) {
        _uiState.value = _uiState.value.copy(selectedTimeRange = range)
        viewModelScope.launch { refresh() }
    }

    private suspend fun refresh() = coroutineScope {
        _uiState.value = _uiState.value.copy(isLoading = true)
        val since = _uiState.value.selectedTimeRange.sinceEpochMs()

        val totalDeferred = async { repository.getStats(username, platform, null, null, since) }

        val categoryStatsDeferred = STAT_CATEGORIES.map { category ->
            async {
                coroutineScope {
                    val currentRating =
                        async { repository.getCurrentRating(username, platform, category) }
                    val startRating = async {
                        repository.getRatingAtStartOfPeriod(username, platform, category, since)
                    }
                    val peak = async { repository.getPeakRating(username, platform, category) }
                    val avgOpp = async {
                        repository.getAvgOpponentRating(username, platform, category, since)
                    }
                    val allStats =
                        async { repository.getStats(username, platform, category, null, since) }
                    val whiteStats =
                        async { repository.getStats(username, platform, category, true, since) }
                    val blackStats =
                        async { repository.getStats(username, platform, category, false, since) }

                    val cur = currentRating.await()
                    val start = startRating.await()
                    CategoryStats(
                        category = category,
                        currentRating = cur,
                        ratingDiff = if (cur != null && start != null) cur - start else null,
                        peakRating = peak.await()?.rating,
                        peakRatingDate = peak.await()?.playedAt,
                        avgOpponentRating = avgOpp.await()?.toInt(),
                        allStats = allStats.await().toGameStats(),
                        whiteStats = whiteStats.await().toGameStats(),
                        blackStats = blackStats.await().toGameStats()
                    )
                }
            }
        }

        _uiState.value = _uiState.value.copy(
            totalGames = totalDeferred.await().played,
            categoryStats = categoryStatsDeferred.awaitAll(),
            isLoading = false
        )
    }

    class Factory(
        private val platform: String,
        private val username: String,
        private val repository: SavedGameRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            return AccountStatsViewModel(platform, username, repository) as T
        }
    }
}