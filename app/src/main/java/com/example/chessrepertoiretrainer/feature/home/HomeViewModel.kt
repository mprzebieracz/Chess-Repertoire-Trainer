package com.example.chessrepertoiretrainer.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.chessrepertoiretrainer.core.activity.ActivityRecorder
import com.example.chessrepertoiretrainer.core.database.entity.DailyActivity
import com.example.chessrepertoiretrainer.feature.puzzles.PuzzleRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface DailyPuzzleState {
    data object Loading : DailyPuzzleState
    data object Fetching : DailyPuzzleState
    data class Available(val rating: Int, val themes: String) : DailyPuzzleState
    data object Solved : DailyPuzzleState
    data class Error(val message: String) : DailyPuzzleState
}

data class HomeUiState(
    val dailyPuzzleState: DailyPuzzleState = DailyPuzzleState.Loading,
    val streak: Int = 0,
    val activityWeeks: List<List<DailyActivity?>> = emptyList()
)

class HomeViewModel(
    private val puzzleRepository: PuzzleRepository,
    private val activityRecorder: ActivityRecorder
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val puzzleJob = async { loadDailyPuzzle() }
            val streakJob = async { loadStreakAndHeatmap() }
            puzzleJob.await()
            streakJob.await()
        }
    }

    fun retry() {
        viewModelScope.launch { loadDailyPuzzle() }
    }

    private suspend fun loadStreakAndHeatmap() {
        val streak = activityRecorder.currentStreakDays()
        val todayMs = ActivityRecorder.todayMs()
        val fromMs = todayMs - 14L * 7 * 24 * 60 * 60 * 1000L
        val activities = activityRecorder.getActivities(fromMs, todayMs)
        val byDate = activities.associateBy { it.date }

        // Build 15 cols × 7 rows. Col 0 = oldest week, col 14 = current week.
        // Row 0 = Monday … row 6 = Sunday (UTC-day buckets).
        val weeks = mutableListOf<List<DailyActivity?>>()
        for (weekOffset in 14 downTo 0) {
            val week = mutableListOf<DailyActivity?>()
            for (dayOffset in 0..6) {
                val dayMs = todayMs - (weekOffset * 7L + dayOffset) * 24 * 60 * 60 * 1000L
                week.add(byDate[dayMs])
            }
            weeks.add(week)
        }

        _uiState.update { it.copy(streak = streak, activityWeeks = weeks) }
    }

    private suspend fun loadDailyPuzzle() {
        _uiState.update { it.copy(dailyPuzzleState = DailyPuzzleState.Loading) }

        val existing = try {
            puzzleRepository.getTodaysPuzzle()
        } catch (e: Exception) {
            _uiState.update { it.copy(dailyPuzzleState = DailyPuzzleState.Error(e.message ?: "Unknown error")) }
            return
        }

        if (existing != null) {
            _uiState.update {
                it.copy(
                    dailyPuzzleState = if (existing.isSolved) DailyPuzzleState.Solved
                    else DailyPuzzleState.Available(existing.rating, existing.themes)
                )
            }
            return
        }

        _uiState.update { it.copy(dailyPuzzleState = DailyPuzzleState.Fetching) }

        val fetched = try {
            puzzleRepository.fetchAndSaveDailyPuzzle()
        } catch (e: Exception) {
            _uiState.update { it.copy(dailyPuzzleState = DailyPuzzleState.Error(e.message ?: "Unknown error")) }
            return
        }

        _uiState.update {
            it.copy(
                dailyPuzzleState = if (fetched != null)
                    DailyPuzzleState.Available(fetched.rating, fetched.themes)
                else DailyPuzzleState.Error("Could not fetch today's puzzle. Check your connection.")
            )
        }
    }

    class Factory(
        private val puzzleRepository: PuzzleRepository,
        private val activityRecorder: ActivityRecorder
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            return HomeViewModel(puzzleRepository, activityRecorder) as T
        }
    }
}
