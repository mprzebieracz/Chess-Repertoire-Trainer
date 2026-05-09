package com.example.chessrepertoiretrainer.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.chessrepertoiretrainer.feature.puzzles.PuzzleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface DailyPuzzleState {
    data object Loading : DailyPuzzleState
    data object Fetching : DailyPuzzleState
    data class Available(val rating: Int, val themes: String) : DailyPuzzleState
    data object Solved : DailyPuzzleState
    data class Error(val message: String) : DailyPuzzleState
}

class HomeViewModel(private val puzzleRepository: PuzzleRepository) : ViewModel() {

    private val _dailyPuzzleState = MutableStateFlow<DailyPuzzleState>(DailyPuzzleState.Loading)
    val dailyPuzzleState: StateFlow<DailyPuzzleState> = _dailyPuzzleState.asStateFlow()

    init {
        checkDailyPuzzle()
    }

    fun retry() {
        checkDailyPuzzle()
    }

    private fun checkDailyPuzzle() {
        viewModelScope.launch {
            _dailyPuzzleState.value = DailyPuzzleState.Loading

            val existing = try {
                puzzleRepository.getTodaysPuzzle()
            }
            catch (e: Exception) {
                _dailyPuzzleState.value = DailyPuzzleState.Error(e.message ?: "Unknown error")
                return@launch
            }

            if (existing != null) {
                _dailyPuzzleState.value = if (existing.isSolved) {
                    DailyPuzzleState.Solved
                }
                else {
                    DailyPuzzleState.Available(existing.rating, existing.themes)
                }
                return@launch
            }

            _dailyPuzzleState.value = DailyPuzzleState.Fetching

            val fetched = try {
                puzzleRepository.fetchAndSaveDailyPuzzle()
            }
            catch (e: Exception) {
                _dailyPuzzleState.value = DailyPuzzleState.Error(e.message ?: "Unknown error")
                return@launch
            }

            _dailyPuzzleState.value = if (fetched != null) {
                DailyPuzzleState.Available(fetched.rating, fetched.themes)
            }
            else {
                DailyPuzzleState.Error("Could not fetch today's puzzle. Check your connection.")
            }
        }
    }

    class Factory(private val puzzleRepository: PuzzleRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            return HomeViewModel(puzzleRepository) as T
        }
    }
}