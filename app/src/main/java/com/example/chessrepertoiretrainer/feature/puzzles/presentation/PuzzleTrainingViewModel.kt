package com.example.chessrepertoiretrainer.feature.puzzles.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.chessrepertoiretrainer.core.database.entity.Puzzle
import com.example.chessrepertoiretrainer.core.chess.controller.DefaultChessBoardController
import com.example.chessrepertoiretrainer.core.chess.domain.convertUciSequenceToSan
import com.example.chessrepertoiretrainer.feature.puzzles.PuzzleRepository
import com.example.chessrepertoiretrainer.feature.repertoire.presentation.viewmodel.MoveTrainingEngine
import com.github.bhlangonijr.chesslib.Side
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PuzzleTrainingUiState(
    val isLoading: Boolean = true,
    val hasUnsolvedPuzzles: Boolean = true,
    val isSessionComplete: Boolean = false,
    val currentPuzzleId: String? = null,
    val currentRating: Int? = null,
    val currentThemes: String? = null,
    val userSideLabel: String? = null,
    val attemptsForCurrent: Int = 0,
    val lastMoveWasCorrect: Boolean? = null,
    val isWaitingForUserMove: Boolean = false,
    val statusMessage: String? = null
)

class PuzzleTrainingViewModel(
    private val repository: PuzzleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PuzzleTrainingUiState())
    val uiState: StateFlow<PuzzleTrainingUiState> = _uiState.asStateFlow()

    val chessController = DefaultChessBoardController()
    private val moveTrainer = MoveTrainingEngine(chessController) { normalizeSan(it) }

    private var currentPuzzle: Puzzle? = null
    private var mySide: Side = Side.WHITE
    private var currentAttempts: Int = 0

    private fun normalizeSan(value: String): String = value.trim().trimEnd('+', '#', '!', '?')

    init {
        // Use SAN-based comparison like repertoire training.
        moveTrainer.setMoveResultListener { result ->
            handleMoveResult(result)
        }

        viewModelScope.launch {
            loadNextPuzzle()
        }
    }

    private suspend fun loadNextPuzzle() {
        _uiState.update {
            it.copy(
                isLoading = true, statusMessage = "Loading puzzle...", lastMoveWasCorrect = null, isWaitingForUserMove = false
            )
        }

        try {
            var attempts = 0
            val maxAttempts = 20

            while (attempts < maxAttempts) {
                attempts++

                val puzzle = repository.getRandomUnsolvedPuzzle()

                if (puzzle == null) {
                    currentPuzzle = null
                    currentAttempts = 0

                    chessController.resetBoard()
                    chessController.allowedMoveSide = null

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            hasUnsolvedPuzzles = false,
                            isSessionComplete = true,
                            currentPuzzleId = null,
                            currentRating = null,
                            currentThemes = null,
                            userSideLabel = null,
                            attemptsForCurrent = 0,
                            lastMoveWasCorrect = null,
                            isWaitingForUserMove = false,
                            statusMessage = "No unsolved puzzles available. You may have already solved today's daily puzzle."
                        )
                    }
                    return
                }
                // Start from this candidate puzzle – raw UCI tokens from the DB.
                val rawTokens = puzzle.moves.split(" ").filter { it.isNotBlank() }
                if (rawTokens.isEmpty()) {
                    // Puzzle without a solution sequence is not useful for training – mark as solved
                    repository.updatePuzzleStats(
                        id = puzzle.id, isSolved = true, attempts = puzzle.attempts
                    )
                    continue
                }

                // Convert the entire UCI solution to SAN using a separate working board.
                val sanMoves = convertUciSequenceToSan(puzzle.fen, rawTokens)
                if (sanMoves.isEmpty()) {
                    repository.updatePuzzleStats(
                        id = puzzle.id, isSolved = true, attempts = puzzle.attempts
                    )
                    continue
                }

                // Single debug log with full UCI and SAN solution for debugging
                Log.d(
                    "PuzzleTrainingViewModel",
                    "Loaded puzzle id=${puzzle.id} rating=${puzzle.rating} " + "fen='${puzzle.fen}' uciSolution='${puzzle.moves}' " + "sanSolution=${
                        sanMoves.joinToString(" ")
                    }"
                )

                // Now initialise the visible training board from the same FEN.
                chessController.loadPositionFromFen(puzzle.fen)
                val board = chessController.getBoard()
                mySide = board.sideToMove
                chessController.allowedMoveSide = mySide

                val shouldBeFlipped = mySide == Side.BLACK
                if (shouldBeFlipped != chessController.isFlipped) {
                    chessController.flipBoard()
                }

                currentPuzzle = puzzle
                currentAttempts = puzzle.attempts

                moveTrainer.reset(
                    MoveTrainingEngine.Config(
                        mySide = mySide, sanMoves = sanMoves
                    )
                )

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        hasUnsolvedPuzzles = true,
                        isSessionComplete = false,
                        currentPuzzleId = puzzle.id,
                        currentRating = puzzle.rating,
                        currentThemes = puzzle.themes,
                        userSideLabel = if (mySide == Side.WHITE) "White" else "Black",
                        attemptsForCurrent = currentAttempts,
                        lastMoveWasCorrect = null,
                        isWaitingForUserMove = true,
                        statusMessage = "Your turn"
                    )
                }
                return
            }

            // Too many invalid puzzles in a row – treat as no valid puzzles available.
            currentPuzzle = null
            currentAttempts = 0

            chessController.resetBoard()
            chessController.allowedMoveSide = null

            _uiState.update {
                it.copy(
                    isLoading = false,
                    hasUnsolvedPuzzles = false,
                    isSessionComplete = true,
                    currentPuzzleId = null,
                    currentRating = null,
                    currentThemes = null,
                    userSideLabel = null,
                    attemptsForCurrent = 0,
                    lastMoveWasCorrect = null,
                    isWaitingForUserMove = false,
                    statusMessage = "No valid puzzles available. Try downloading puzzles again later."
                )
            }
        }
        catch (e: Exception) {
            currentPuzzle = null
            currentAttempts = 0

            chessController.resetBoard()
            chessController.allowedMoveSide = null

            _uiState.update {
                it.copy(
                    isLoading = false,
                    hasUnsolvedPuzzles = false,
                    isSessionComplete = true,
                    currentPuzzleId = null,
                    currentRating = null,
                    currentThemes = null,
                    userSideLabel = null,
                    attemptsForCurrent = 0,
                    lastMoveWasCorrect = null,
                    isWaitingForUserMove = false,
                    statusMessage = e.message ?: "Error loading puzzles"
                )
            }
        }
    }

    fun nextPuzzle() {
        if (currentPuzzle == null) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isWaitingForUserMove = false, statusMessage = "Loading next puzzle..."
                )
            }
            loadNextPuzzle()
        }
    }

    fun showSolution() {
        if (currentPuzzle == null) return

        viewModelScope.launch {
            val finished = moveTrainer.playSolutionStep()
            if (finished) {
                markPuzzleSolved()
            }
            else {
                _uiState.update {
                    it.copy(
                        statusMessage = "Solution move played.", isWaitingForUserMove = true
                    )
                }
            }
        }
    }

    fun showHint() {
        if (currentPuzzle == null) return

        val square = moveTrainer.computeHintSquare()
        if (square != null) {
            chessController.markedSquare = square
            _uiState.update {
                it.copy(statusMessage = "Hint: highlighted the piece to move.")
            }
        }
        else {
            _uiState.update {
                it.copy(statusMessage = "No hint available right now.")
            }
        }
    }

    private fun handleMoveResult(result: MoveTrainingEngine.MoveResult) {
        val puzzle = currentPuzzle ?: return

        when (result) {
            is MoveTrainingEngine.MoveResult.Correct -> {
                _uiState.update {
                    it.copy(
                        lastMoveWasCorrect = true, statusMessage = "Correct!"
                    )
                }

                viewModelScope.launch {
                    moveTrainer.advanceOpponentReplies()
                    if (moveTrainer.isSequenceComplete()) {
                        markPuzzleSolved()
                    }
                    else {
                        _uiState.update {
                            it.copy(
                                isWaitingForUserMove = true, statusMessage = "Your turn"
                            )
                        }
                    }
                }
            }

            is MoveTrainingEngine.MoveResult.Incorrect -> {
                currentAttempts++

                viewModelScope.launch {
                    repository.updatePuzzleStats(
                        id = puzzle.id, isSolved = false, attempts = currentAttempts
                    )
                }

                _uiState.update {
                    it.copy(
                        lastMoveWasCorrect = false,
                        attemptsForCurrent = currentAttempts,
                        isWaitingForUserMove = true,
                        statusMessage = "Incorrect, try again"
                    )
                }

                chessController.navigateBack()
            }
        }
    }

    private fun markPuzzleSolved() {
        val puzzle = currentPuzzle ?: return

        viewModelScope.launch {
            // For now we keep the puzzle marked as unsolved so that you can
            // replay it over and over to verify the training logic.
            repository.updatePuzzleStats(
                id = puzzle.id, isSolved = false, attempts = currentAttempts
            )

            _uiState.update {
                it.copy(
                    isWaitingForUserMove = false, statusMessage = "Puzzle complete. Press Next puzzle to replay."
                )
            }
        }
    }


    class Factory(
        private val repository: PuzzleRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            return PuzzleTrainingViewModel(repository) as T
        }
    }
}


