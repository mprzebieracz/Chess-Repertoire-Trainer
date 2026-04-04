package com.example.chessrepertoiretrainer.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.chessrepertoiretrainer.data.PuzzleRepository
import com.example.chessrepertoiretrainer.database.entities.Puzzle
import com.example.chessrepertoiretrainer.ui.components.chess.DefaultChessBoardController
import com.example.chessrepertoiretrainer.ui.components.chess.convertUciSequenceToSan
import com.example.chessrepertoiretrainer.ui.components.chess.toSan
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Side
import kotlinx.coroutines.delay
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

    private var currentPuzzle: Puzzle? = null
    private var solutionMoves: List<String> = emptyList()
    private var currentMoveIndex: Int = 0
    private var mySide: Side = Side.WHITE
    private var isAutoPlaying: Boolean = false
    private var currentAttempts: Int = 0

    private fun normalizeSan(value: String): String =
        value.trim().trimEnd('+', '#', '!', '?')

    init {
        // Use SAN-based comparison like repertoire training.
        chessController.onMoveListener = { _, san, _ ->
            handleMoveFromBoard(san)
        }

        viewModelScope.launch {
            loadNextPuzzle()
        }
    }

    private suspend fun loadNextPuzzle() {
        _uiState.update {
            it.copy(
                isLoading = true,
                statusMessage = "Loading puzzle...",
                lastMoveWasCorrect = null,
                isWaitingForUserMove = false
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
                    solutionMoves = emptyList()
                    currentMoveIndex = 0
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
                        id = puzzle.id,
                        isSolved = true,
                        attempts = puzzle.attempts
                    )
                    continue
                }

                // Convert the entire UCI solution to SAN using a separate working board.
                val sanMoves = convertUciSequenceToSan(puzzle.fen, rawTokens)
                if (sanMoves.isEmpty()) {
                    repository.updatePuzzleStats(
                        id = puzzle.id,
                        isSolved = true,
                        attempts = puzzle.attempts
                    )
                    continue
                }

                // Single debug log with full UCI and SAN solution for debugging
                Log.d(
                    "PuzzleTrainingViewModel",
                    "Loaded puzzle id=${puzzle.id} rating=${puzzle.rating} " +
                        "fen='${puzzle.fen}' uciSolution='${puzzle.moves}' " +
                        "sanSolution=${sanMoves.joinToString(" ")}"
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
                solutionMoves = sanMoves
                currentMoveIndex = 0
                currentAttempts = puzzle.attempts

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
            solutionMoves = emptyList()
            currentMoveIndex = 0
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
        } catch (e: Exception) {
            currentPuzzle = null
            solutionMoves = emptyList()
            currentMoveIndex = 0
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
                    isWaitingForUserMove = false,
                    statusMessage = "Loading next puzzle..."
                )
            }
            loadNextPuzzle()
        }
    }

    fun showSolution() {
        if (currentPuzzle == null) return
        if (solutionMoves.isEmpty()) return
        if (currentMoveIndex >= solutionMoves.size) return

    viewModelScope.launch {
      val board = chessController.getBoard()

      // Only allow showing the solution when it's the user's turn, to avoid
      // racing with ongoing automatic opponent replies.
      if (board.sideToMove != mySide) {
        return@launch
      }

      // Play only the next solution move using SAN, like in repertoire training.
      val targetSan = solutionMoves[currentMoveIndex]
      val move = board.legalMoves().firstOrNull { legal ->
        board.toSan(legal) == targetSan
      }
      if (move == null) {
        _uiState.update {
          it.copy(
            statusMessage = "Unable to show solution move from this position.",
            isWaitingForUserMove = true
          )
        }
        return@launch
      }

      delay(300)
      isAutoPlaying = true
      chessController.onMove(move)
      isAutoPlaying = false

      currentMoveIndex++

      if (currentMoveIndex >= solutionMoves.size) {
        markPuzzleSolved()
      } else {
        _uiState.update {
          it.copy(
            statusMessage = "Solution move played.",
            isWaitingForUserMove = board.sideToMove == mySide
          )
        }
      }
    }
    }

  private fun handleMoveFromBoard(san: String) {
        if (isAutoPlaying) return

        val puzzle = currentPuzzle ?: return
        if (solutionMoves.isEmpty()) return
        if (currentMoveIndex !in solutionMoves.indices) return

    val expectedSan = solutionMoves[currentMoveIndex]
    val isCorrect = normalizeSan(san) == normalizeSan(expectedSan)

        if (isCorrect) {
            currentMoveIndex++

            _uiState.update {
                it.copy(
                    lastMoveWasCorrect = true,
                    statusMessage = "Correct!"
                )
            }

            viewModelScope.launch {
                advanceThroughOpponentReplies()
                if (currentMoveIndex >= solutionMoves.size) {
                    markPuzzleSolved()
                } else {
                    _uiState.update {
                        it.copy(
                            isWaitingForUserMove = true,
                            statusMessage = "Your turn"
                        )
                    }
                }
            }
        } else {
            currentAttempts++

            viewModelScope.launch {
                repository.updatePuzzleStats(
                    id = puzzle.id,
                    isSolved = false,
                    attempts = currentAttempts
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

    private suspend fun advanceThroughOpponentReplies() {
        val puzzle = currentPuzzle ?: return
        if (solutionMoves.isEmpty()) return

        val board = chessController.getBoard()

        while (currentMoveIndex < solutionMoves.size && board.sideToMove != mySide) {
            val targetSan = solutionMoves[currentMoveIndex]
            val legalMove = board.legalMoves().firstOrNull { move ->
                board.toSan(move) == targetSan
            } ?: break

            delay(500)
            isAutoPlaying = true
            chessController.onMove(legalMove)
            isAutoPlaying = false

            currentMoveIndex++
        }
    }

    private fun markPuzzleSolved() {
        val puzzle = currentPuzzle ?: return

        viewModelScope.launch {
            // For now we keep the puzzle marked as unsolved so that you can
            // replay it over and over to verify the training logic.
            repository.updatePuzzleStats(
                id = puzzle.id,
                isSolved = false,
                attempts = currentAttempts
            )

            _uiState.update {
                it.copy(
                    isWaitingForUserMove = false,
                    statusMessage = "Puzzle complete. Press Next puzzle to replay."
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


