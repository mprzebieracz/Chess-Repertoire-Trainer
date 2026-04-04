package com.example.chessrepertoiretrainer.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.chessrepertoiretrainer.data.PuzzleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PuzzlesUiState(
    val isLoading: Boolean = true,
    val unsolvedCount: Int = 0,
    val errorMessage: String? = null
)

class PuzzlesViewModel(
    private val repository: PuzzleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PuzzlesUiState())
    val uiState: StateFlow<PuzzlesUiState> = _uiState.asStateFlow()

    // In daily mode we only need at most one unsolved puzzle available.
    private val minUnsolvedPuzzles = 1

    init {
        viewModelScope.launch {
            ensurePuzzlesAvailable()
        }
    }

    private suspend fun ensurePuzzlesAvailable() {
        try {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            val finalCount = repository.ensureMinUnsolvedPuzzles(minUnsolvedPuzzles)
            Log.d(
                "PuzzlesViewModel",
                "Unsolved puzzles after ensuring minimum: $finalCount"
            )
            _uiState.value = PuzzlesUiState(
                isLoading = false,
                unsolvedCount = finalCount,
                errorMessage = null
            )
        } catch (e: Exception) {
            // Even if ensuring the minimum failed (e.g. network error), we may
            // still have some puzzles already stored locally.
            val existingCount = try {
                repository.getUnsolvedCount()
            } catch (_: Exception) {
                0
            }

            _uiState.value = PuzzlesUiState(
                isLoading = false,
                unsolvedCount = existingCount,
                errorMessage = e.message ?: "Unknown error"
            )
        }
    }

    class Factory(
        private val repository: PuzzleRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            return PuzzlesViewModel(repository) as T
        }
    }
}

