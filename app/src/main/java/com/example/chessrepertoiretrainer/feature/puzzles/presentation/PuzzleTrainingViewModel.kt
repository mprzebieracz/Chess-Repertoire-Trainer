package com.example.chessrepertoiretrainer.feature.puzzles.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.chessrepertoiretrainer.core.chess.controller.DefaultChessBoardController
import com.example.chessrepertoiretrainer.core.chess.pgn.uci.convertUciSequenceToSan
import com.example.chessrepertoiretrainer.core.chess.training.MoveTrainingEngine
import com.example.chessrepertoiretrainer.core.database.entity.Puzzle
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

data class PuzzleTrainingUiState(
    val isLoading: Boolean = true,
    val isSessionComplete: Boolean = false,
    val currentRating: Int? = null,
    val currentThemes: String? = null,
    val userSideLabel: String? = null,
    val attemptsForCurrent: Int = 0,
    val lastMoveWasCorrect: Boolean? = null,
    val isWaitingForUserMove: Boolean = false,
    val statusMessage: String? = null
)

class PuzzleTrainingViewModel(private val repository: PuzzleRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(PuzzleTrainingUiState())
    val uiState: StateFlow<PuzzleTrainingUiState> = _uiState.asStateFlow()

    val chessController = DefaultChessBoardController()
    private val moveTrainer =
        MoveTrainingEngine(chessController) { it.trim().trimEnd('+', '#', '!', '?') }

    private val markPuzzleSolvedUseCase = MarkPuzzleSolvedUseCase(repository)
    private val updatePuzzleAttemptsUseCase = UpdatePuzzleAttemptsUseCase(repository)
    private val checkPuzzleMoveUseCase = CheckPuzzleMoveUseCase()

    private var currentPuzzle: Puzzle? = null
    private var currentAttempts: Int = 0

    init {
        moveTrainer.setMoveResultListener { result -> handleMoveResult(result) }
        viewModelScope.launch { loadTodaysPuzzle() }
    }

    private suspend fun loadTodaysPuzzle() {
        _uiState.update { it.copy(isLoading = true, statusMessage = "Loading puzzle...") }

        val puzzle = try {
            repository.getTodaysPuzzle()
        } catch (e: Exception) {
            setErrorState(e.message ?: "Error loading puzzle")
            return
        }

        if (puzzle == null) {
            setErrorState("No puzzle available. Check your connection and try again.")
            return
        }

        val sanMoves =
            convertUciSequenceToSan(puzzle.fen, puzzle.moves.split(" ").filter { it.isNotBlank() })

        if (sanMoves.isEmpty()) {
            setErrorState("Puzzle data is invalid.")
            return
        }

        val mySide = Board().apply { loadFromFen(puzzle.fen) }.sideToMove

        chessController.loadPositionFromFen(puzzle.fen)
        chessController.allowedMoveSide = mySide
        chessController.orientForSide(mySide)

        currentPuzzle = puzzle
        currentAttempts = puzzle.attempts
        moveTrainer.reset(MoveTrainingEngine.Config(mySide = mySide, sanMoves = sanMoves))

        _uiState.update {
            it.copy(
                isLoading = false,
                isSessionComplete = false,
                currentRating = puzzle.rating,
                currentThemes = puzzle.themes,
                userSideLabel = if (mySide == Side.WHITE) "White" else "Black",
                attemptsForCurrent = currentAttempts,
                lastMoveWasCorrect = null,
                isWaitingForUserMove = true,
                statusMessage = "Your turn"
            )
        }
    }

    fun showSolution() {
        if (currentPuzzle == null) return
        viewModelScope.launch {
            val finished = moveTrainer.playSolutionStep()
            if (finished) {
                markSolved()
            } else {
                _uiState.update {
                    it.copy(statusMessage = "Solution move played.", isWaitingForUserMove = true)
                }
            }
        }
    }

    fun showHint() {
        if (currentPuzzle == null) return
        val square = moveTrainer.computeHintSquare()
        if (square != null) {
            chessController.markedSquare = square
            _uiState.update { it.copy(statusMessage = "Hint: highlighted the piece to move.") }
        } else {
            _uiState.update { it.copy(statusMessage = "No hint available right now.") }
        }
    }

    private fun handleMoveResult(result: MoveTrainingEngine.MoveResult) {
        val puzzle = currentPuzzle ?: return

        when (checkPuzzleMoveUseCase(result)) {
            MoveCheckResult.Correct -> {
                _uiState.update { it.copy(lastMoveWasCorrect = true, statusMessage = "Correct!") }
                viewModelScope.launch {
                    moveTrainer.advanceOpponentReplies()
                    if (moveTrainer.isSequenceComplete()) {
                        markSolved()
                    } else {
                        _uiState.update {
                            it.copy(isWaitingForUserMove = true, statusMessage = "Your turn")
                        }
                    }
                }
            }

            MoveCheckResult.Incorrect -> {
                currentAttempts++
                viewModelScope.launch {
                    updatePuzzleAttemptsUseCase(id = puzzle.id, attempts = currentAttempts)
                }
                _uiState.update {
                    it.copy(
                        lastMoveWasCorrect = false,
                        attemptsForCurrent = currentAttempts,
                        isWaitingForUserMove = true,
                        statusMessage = "Incorrect, try again"
                    )
                }
                chessController.loadPositionFromFen((result as MoveTrainingEngine.MoveResult.Incorrect).fenBefore)
            }
        }
    }

    private fun markSolved() {
        val puzzle = currentPuzzle ?: return
        viewModelScope.launch {
            markPuzzleSolvedUseCase(id = puzzle.id, attempts = currentAttempts)
            _uiState.update {
                it.copy(
                    isSessionComplete = true,
                    isWaitingForUserMove = false,
                    statusMessage = "Daily puzzle complete!"
                )
            }
        }
    }

    private fun setErrorState(message: String) {
        currentPuzzle = null
        chessController.resetBoard()
        _uiState.update {
            it.copy(isLoading = false, isSessionComplete = true, statusMessage = message)
        }
    }

    class Factory(private val repository: PuzzleRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            return PuzzleTrainingViewModel(repository) as T
        }
    }
}