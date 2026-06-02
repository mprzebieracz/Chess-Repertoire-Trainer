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
    val isError: Boolean = false,
    val solvedInSession: Int = 0,
    val currentRating: Int? = null,
    val currentOpeningFamily: String? = null,
    val userSideLabel: String? = null,
    val lastMoveWasCorrect: Boolean? = null,
    val isWaitingForUserMove: Boolean = false,
    val statusMessage: String? = null,
)

class OpeningPuzzleSessionViewModel(
    private val repository: PuzzleRepository,
    private val selectedOpenings: List<RepertoireOpening>,
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

    private val families = selectedOpenings.map { it.family }
    private var solvedInSession = 0
    private var currentPuzzle: Puzzle? = null
    private var currentAttempts: Int = 0

    init {
        moveTrainer.setMoveResultListener(::handleMoveResult)
        viewModelScope.launch { loadNextPuzzle() }
    }

    private suspend fun loadNextPuzzle() {
        _uiState.update { it.copy(isLoading = true, lastMoveWasCorrect = null) }

        var puzzle = repository.getNextUnsolvedPuzzleForFamilies(families)
        if (puzzle == null) {
            repository.fetchAndSaveOpeningPuzzles(selectedOpenings)
            puzzle = repository.getNextUnsolvedPuzzleForFamilies(families)
        }
        if (puzzle == null) {
            _uiState.update { it.copy(isLoading = false, isError = true) }
            return
        }

        // Loop to skip invalid puzzles (bad FEN/moves) rather than recursing
        var attempts = 0
        while (attempts < 10) {
            attempts++
            val sanMoves = convertUciSequenceToSan(
                puzzle!!.fen, puzzle.moves.split(" ").filter { it.isNotBlank() }
            )
            if (sanMoves.isNotEmpty()) {
                presentPuzzle(puzzle, sanMoves)
                return
            }
            // Mark invalid puzzle as solved to remove it from the pool
            markPuzzleSolvedUseCase(id = puzzle.id, attempts = 0)
            puzzle = repository.getNextUnsolvedPuzzleForFamilies(families) ?: break
        }

        _uiState.update { it.copy(isLoading = false, isError = true) }
    }

    private fun presentPuzzle(puzzle: Puzzle, sanMoves: List<String>) {
        currentPuzzle = puzzle
        currentAttempts = puzzle.attempts

        val mySide = Board().apply { loadFromFen(puzzle.fen) }.sideToMove
        chessController.loadPositionFromFen(puzzle.fen)
        chessController.allowedMoveSide = mySide
        chessController.orientForSide(mySide)
        chessController.markedSquare = null
        moveTrainer.reset(MoveTrainingEngine.Config(mySide = mySide, sanMoves = sanMoves))

        _uiState.update {
            it.copy(
                isLoading = false,
                isError = false,
                solvedInSession = solvedInSession,
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
        solvedInSession++
        viewModelScope.launch {
            markPuzzleSolvedUseCase(id = puzzle.id, attempts = currentAttempts)
        }
        ensurePoolFilled()
        viewModelScope.launch { loadNextPuzzle() }
    }

    private fun ensurePoolFilled() {
        viewModelScope.launch {
            for (opening in selectedOpenings) {
                if (repository.countUnsolvedForFamily(opening.family) < 3) {
                    repository.fetchAndSaveOpeningPuzzles(listOf(opening))
                }
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
                solvedInSession++
                markPuzzleSolvedUseCase(id = puzzle.id, attempts = currentAttempts)
                ensurePoolFilled()
                loadNextPuzzle()
            }
        }
    }

    class Factory(
        private val repository: PuzzleRepository,
        private val selectedOpenings: List<RepertoireOpening>,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
            OpeningPuzzleSessionViewModel(repository, selectedOpenings) as T
    }
}
