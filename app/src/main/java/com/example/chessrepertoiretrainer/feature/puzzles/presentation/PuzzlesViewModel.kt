package com.example.chessrepertoiretrainer.feature.puzzles.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.chessrepertoiretrainer.feature.puzzles.PuzzleRepository
import com.example.chessrepertoiretrainer.feature.puzzles.domain.config.PuzzleTrainingConfig
import com.example.chessrepertoiretrainer.feature.puzzles.domain.usecase.EnsureMinUnsolvedUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PuzzlesUiState(
    val isLoading: Boolean = true, val unsolvedCount: Int = 0, val errorMessage: String? = null
)

class PuzzlesViewModel(
    private val ensureMinUnsolved: EnsureMinUnsolvedUseCase,
    private val repository: PuzzleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PuzzlesUiState())
    val uiState: StateFlow<PuzzlesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            ensurePuzzlesAvailable()
        }
    }

    private suspend fun ensurePuzzlesAvailable() {
        try {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            val finalCount = ensureMinUnsolved()
            Log.d(
                "PuzzlesViewModel", "Unsolved puzzles after ensuring minimum: $finalCount"
            )
            _uiState.value = PuzzlesUiState(
                isLoading = false, unsolvedCount = finalCount, errorMessage = null
            )
        }
        catch (e: Exception) {
            // Even if ensuring the minimum failed (e.g. network error), we may
            // still have some puzzles already stored locally.
            val existingCount = try {
                repository.getUnsolvedCount()
            }
            catch (_: Exception) {
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
            val config = PuzzleTrainingConfig()
            val ensureMinUnsolved = EnsureMinUnsolvedUseCase(repository, config)
            return PuzzlesViewModel(ensureMinUnsolved, repository) as T
        }
    }
}

