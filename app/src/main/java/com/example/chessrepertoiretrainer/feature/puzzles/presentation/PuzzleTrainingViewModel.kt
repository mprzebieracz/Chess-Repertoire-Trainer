package com.example.chessrepertoiretrainer.feature.puzzles.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.chessrepertoiretrainer.core.chess.controller.DefaultChessBoardController
import com.example.chessrepertoiretrainer.core.database.entity.Puzzle
import com.example.chessrepertoiretrainer.feature.puzzles.PuzzleRepository
import com.example.chessrepertoiretrainer.feature.puzzles.domain.config.PuzzleTrainingConfig
import com.example.chessrepertoiretrainer.feature.puzzles.domain.model.MoveCheckResult
import com.example.chessrepertoiretrainer.feature.puzzles.domain.model.PuzzleSessionState
import com.example.chessrepertoiretrainer.feature.puzzles.domain.usecase.CheckPuzzleMoveUseCase
import com.example.chessrepertoiretrainer.feature.puzzles.domain.usecase.LoadNextPuzzleUseCase
import com.example.chessrepertoiretrainer.feature.puzzles.domain.usecase.MarkPuzzleSolvedUseCase
import com.example.chessrepertoiretrainer.feature.puzzles.domain.usecase.UpdatePuzzleAttemptsUseCase
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
    private val loadNextPuzzleUseCase: LoadNextPuzzleUseCase,
    private val updatePuzzleAttemptsUseCase: UpdatePuzzleAttemptsUseCase,
    private val markPuzzleSolvedUseCase: MarkPuzzleSolvedUseCase,
    private val checkPuzzleMoveUseCase: CheckPuzzleMoveUseCase
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
                isLoading = true,
                statusMessage = "Loading puzzle...",
                lastMoveWasCorrect = null,
                isWaitingForUserMove = false
            )
        }

        try {
            val session = loadNextPuzzleUseCase()
            if (session == null) {
                setNoPuzzlesState("No unsolved puzzles available. You may have already solved today's daily puzzle.")
                return
            }

            applySession(session)
        } catch (e: Exception) {
            setNoPuzzlesState(e.message ?: "Error loading puzzles")
        }
    }

    private fun applySession(session: PuzzleSessionState) {
        val puzzle = session.puzzle

        Log.d(
            "PuzzleTrainingViewModel",
            "Loaded puzzle id=${puzzle.id} rating=${puzzle.rating} fen='${puzzle.fen}' sanSolution=${session.sanMoves.joinToString(" ")}"
        )

        chessController.loadPositionFromFen(puzzle.fen)
        mySide = session.mySide
        chessController.allowedMoveSide = mySide

        val shouldBeFlipped = mySide == Side.BLACK
        if (shouldBeFlipped != chessController.isFlipped) {
            chessController.flipBoard()
        }

        currentPuzzle = puzzle
        currentAttempts = puzzle.attempts

        moveTrainer.reset(MoveTrainingEngine.Config(mySide = mySide, sanMoves = session.sanMoves))

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
    }

    private fun setNoPuzzlesState(message: String) {
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
                statusMessage = message
            )
        }
    }

    fun nextPuzzle() {
        if (currentPuzzle == null) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(isWaitingForUserMove = false, statusMessage = "Loading next puzzle...")
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
                        markPuzzleSolved()
                    } else {
                        _uiState.update { it.copy(isWaitingForUserMove = true, statusMessage = "Your turn") }
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

                chessController.navigateBack()
            }
        }
    }

    private fun markPuzzleSolved() {
        val puzzle = currentPuzzle ?: return

        viewModelScope.launch {
            markPuzzleSolvedUseCase(id = puzzle.id, attempts = currentAttempts)
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
            val config = PuzzleTrainingConfig()
            val loadNextPuzzleUseCase = LoadNextPuzzleUseCase(repository, config)
            val updatePuzzleAttemptsUseCase = UpdatePuzzleAttemptsUseCase(repository)
            val markPuzzleSolvedUseCase = MarkPuzzleSolvedUseCase(repository)
            val checkPuzzleMoveUseCase = CheckPuzzleMoveUseCase()

            return PuzzleTrainingViewModel(
                loadNextPuzzleUseCase = loadNextPuzzleUseCase,
                updatePuzzleAttemptsUseCase = updatePuzzleAttemptsUseCase,
                markPuzzleSolvedUseCase = markPuzzleSolvedUseCase,
                checkPuzzleMoveUseCase = checkPuzzleMoveUseCase
            ) as T
        }
    }
}


