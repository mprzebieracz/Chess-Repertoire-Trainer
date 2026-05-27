package com.example.chessrepertoiretrainer.feature.puzzles.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.chessrepertoiretrainer.core.chess.controller.BoardAnnotations
import com.example.chessrepertoiretrainer.core.chess.controller.DefaultChessBoardController
import com.example.chessrepertoiretrainer.core.chess.pgn.uci.convertUciSequenceToSan
import com.example.chessrepertoiretrainer.core.chess.training.MoveTrainingEngine
import com.example.chessrepertoiretrainer.core.database.entity.Puzzle
import com.example.chessrepertoiretrainer.core.database.entity.RepertoireOpening
import com.example.chessrepertoiretrainer.feature.puzzles.PuzzleRepository
import com.example.chessrepertoiretrainer.feature.puzzles.domain.model.MoveCheckResult
import com.example.chessrepertoiretrainer.feature.puzzles.domain.usecase.CheckPuzzleMoveUseCase
import com.example.chessrepertoiretrainer.feature.puzzles.domain.usecase.MarkPuzzleSolvedUseCase
import com.example.chessrepertoiretrainer.feature.puzzles.domain.usecase.UpdatePuzzleAttemptsUseCase
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Side
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OpeningPuzzleSessionUiState(
    val isLoading: Boolean = true,
    val isSessionComplete: Boolean = false,
    val currentPuzzleNumber: Int = 0,
    val totalPuzzles: Int = 0,
    val currentRating: Int? = null,
    val currentOpeningFamily: String? = null,
    val userSideLabel: String? = null,
    val lastMoveWasCorrect: Boolean? = null,
    val isWaitingForUserMove: Boolean = false,
    val statusMessage: String? = null,
)

class OpeningPuzzleSessionViewModel(
    private val repository: PuzzleRepository,
    private val selectedFamilies: List<String>,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OpeningPuzzleSessionUiState())
    val uiState: StateFlow<OpeningPuzzleSessionUiState> = _uiState.asStateFlow()

    val chessController = DefaultChessBoardController()
    val annotations = BoardAnnotations()

    private val moveTrainer = MoveTrainingEngine(chessController, viewModelScope) {
        it.trim().trimEnd('+', '#', '!', '?')
    }

    private val markPuzzleSolvedUseCase = MarkPuzzleSolvedUseCase(repository)
    private val updatePuzzleAttemptsUseCase = UpdatePuzzleAttemptsUseCase(repository)
    private val checkPuzzleMoveUseCase = CheckPuzzleMoveUseCase()

    private var puzzleQueue: List<Puzzle> = emptyList()
    private var currentIndex: Int = -1
    private var currentPuzzle: Puzzle? = null
    private var currentAttempts: Int = 0

    init {
        moveTrainer.setMoveResultListener(::handleMoveResult)
        viewModelScope.launch { loadSession() }
    }

    private suspend fun loadSession() {
        _uiState.update { it.copy(isLoading = true) }
        try {
            val puzzles = repository.getUnsolvedPuzzlesForFamilies(selectedFamilies).shuffled()
            if (puzzles.isEmpty()) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isSessionComplete = true,
                        statusMessage = "No unsolved puzzles for the selected openings.",
                    )
                }
                return
            }
            puzzleQueue = puzzles
            startPuzzle(0)
        } catch (_: Exception) {
            _uiState.update { it.copy(isLoading = false, isSessionComplete = true, statusMessage = "Failed to load puzzles.") }
        }
    }

    private fun startPuzzle(index: Int) {
        if (index >= puzzleQueue.size) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isSessionComplete = true,
                    isWaitingForUserMove = false,
                    statusMessage = "Session complete!",
                )
            }
            return
        }

        val puzzle = puzzleQueue[index]
        currentIndex = index
        currentPuzzle = puzzle
        currentAttempts = puzzle.attempts

        val sanMoves = convertUciSequenceToSan(
            puzzle.fen, puzzle.moves.split(" ").filter { it.isNotBlank() }
        )
        if (sanMoves.isEmpty()) {
            // Skip invalid puzzle and advance
            startPuzzle(index + 1)
            return
        }

        val mySide = Board().apply { loadFromFen(puzzle.fen) }.sideToMove

        chessController.loadPositionFromFen(puzzle.fen)
        chessController.allowedMoveSide = mySide
        chessController.orientForSide(mySide)
        chessController.markedSquare = null
        moveTrainer.reset(MoveTrainingEngine.Config(mySide = mySide, sanMoves = sanMoves))

        _uiState.update {
            it.copy(
                isLoading = false,
                isSessionComplete = false,
                currentPuzzleNumber = index + 1,
                totalPuzzles = puzzleQueue.size,
                currentRating = puzzle.rating,
                currentOpeningFamily = puzzle.openingFamily,
                userSideLabel = if (mySide == Side.WHITE) "White" else "Black",
                lastMoveWasCorrect = null,
                isWaitingForUserMove = true,
                statusMessage = null,
            )
        }
    }

    private fun handleMoveResult(result: MoveTrainingEngine.MoveResult) {
        val puzzle = currentPuzzle ?: return

        when (checkPuzzleMoveUseCase(result)) {
            MoveCheckResult.Correct -> {
                val isComplete = (result as MoveTrainingEngine.MoveResult.Correct).isComplete
                _uiState.update {
                    it.copy(
                        lastMoveWasCorrect = true,
                        isWaitingForUserMove = !isComplete,
                        statusMessage = if (isComplete) null else null,
                    )
                }
                if (isComplete) solveCurrent(puzzle)
            }

            MoveCheckResult.Incorrect -> {
                currentAttempts++
                viewModelScope.launch {
                    updatePuzzleAttemptsUseCase(id = puzzle.id, attempts = currentAttempts)
                }
                _uiState.update {
                    it.copy(
                        lastMoveWasCorrect = false,
                        isWaitingForUserMove = true,
                        statusMessage = "Incorrect, try again",
                    )
                }
            }
        }
    }

    private fun solveCurrent(puzzle: Puzzle) {
        viewModelScope.launch {
            markPuzzleSolvedUseCase(id = puzzle.id, attempts = currentAttempts)
        }
        maybeRefillPool(puzzle.openingFamily)
        startPuzzle(currentIndex + 1)
    }

    private fun maybeRefillPool(family: String?) {
        if (family.isNullOrBlank()) return
        viewModelScope.launch {
            val remaining = repository.countUnsolvedForFamily(family)
            if (remaining < 3) {
                val opening = repository.getOpeningByFamily(family)
                    ?: RepertoireOpening(family = family)
                repository.fetchAndSaveOpeningPuzzles(listOf(opening))
            }
        }
    }

    fun showHint() {
        val square = moveTrainer.computeHintSquare() ?: return
        chessController.markedSquare = square
    }

    fun showSolution() {
        viewModelScope.launch {
            val finished = moveTrainer.playSolutionStep()
            if (finished) {
                val puzzle = currentPuzzle ?: return@launch
                markPuzzleSolvedUseCase(id = puzzle.id, attempts = currentAttempts)
                maybeRefillPool(puzzle.openingFamily)
                startPuzzle(currentIndex + 1)
            }
        }
    }

    class Factory(
        private val repository: PuzzleRepository,
        private val selectedFamilies: List<String>,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
            OpeningPuzzleSessionViewModel(repository, selectedFamilies) as T
    }
}
