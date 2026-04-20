package com.example.chessrepertoiretrainer.feature.repertoire.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.chessrepertoiretrainer.core.database.entity.Line
import com.example.chessrepertoiretrainer.core.database.entity.LineMove
import com.example.chessrepertoiretrainer.core.chess.controller.DefaultChessBoardController
import com.example.chessrepertoiretrainer.feature.repertoire.domain.RepertoireRepository
import com.github.bhlangonijr.chesslib.Side
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TrainingUiState(
    val isLoading: Boolean = true,
    val isSessionEmpty: Boolean = false,
    val isSessionComplete: Boolean = false,
    val currentLineName: String? = null,
    val currentLineNumber: Int = 0,
    val totalLines: Int = 0,
    val myColor: String? = null,
    val lastMoveWasCorrect: Boolean? = null,
    val lastUserSan: String? = null,
    val lastExpectedSan: String? = null,
    val isWaitingForUserMove: Boolean = false,
    val statusMessage: String? = null
)

class TrainingViewModel(
    private val repertoireRepository: RepertoireRepository, savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrainingUiState())
    val uiState: StateFlow<TrainingUiState> = _uiState.asStateFlow()

    val chessController = DefaultChessBoardController()
    private val moveTrainer = MoveTrainingEngine(chessController) { normalizeSan(it) }

    private val chapterId: Int? = savedStateHandle["chapterId"]
    private val lineId: Int? = savedStateHandle["lineId"]
    private var lines: List<Line> = emptyList()
    private var currentLineIndex: Int = -1
    private var currentLineMoves: List<LineMove> = emptyList()
    private var mySide: Side = Side.WHITE

    private fun normalizeSan(value: String): String {
        return value.trim().trimEnd('+', '#')
    }

    init {
        moveTrainer.setMoveResultListener { result ->
            handleMoveResult(result)
        }

        viewModelScope.launch {
            // Single-line training mode (used from learn flow)
            if (lineId != null) {
                val line = repertoireRepository.getLineById(lineId)

                if (line == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false, isSessionEmpty = true, isSessionComplete = false, statusMessage = "Line not found"
                        )
                    }
                    return@launch
                }

                lines = listOf(line)
                startLine(0)
                return@launch
            }

            // Chapter-wide or review-based training
            val flow = if (chapterId != null) {
                repertoireRepository.getLinesForChapter(chapterId)
            }
            else {
                val allLinesTime = Long.MAX_VALUE
                repertoireRepository.getLinesToReview(allLinesTime)
            }
            flow.collect { loadedLines ->
                if (loadedLines.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isLoading = false, isSessionEmpty = true, isSessionComplete = false, totalLines = 0, currentLineName = null
                        )
                    }
                    return@collect
                }

                if (currentLineIndex == -1) {
                    // First time we load lines for this session.
                    // For chapter-based training, we want to train lines in a random
                    // permutation so that the user doesn't always see them in the
                    // same order. For review-based training we keep the original
                    // ordering from the DAO.
                    lines = if (chapterId != null) {
                        loadedLines.shuffled()
                    }
                    else {
                        loadedLines
                    }
                    startLine(0)
                }
                else {
                    _uiState.update { state ->
                        state.copy(totalLines = lines.size)
                    }
                }
            }
        }
    }

    private fun handleMoveResult(result: MoveTrainingEngine.MoveResult) {
        when (result) {
            is MoveTrainingEngine.MoveResult.Correct -> {
                _uiState.update {
                    it.copy(
                        lastMoveWasCorrect = true, lastUserSan = result.userSan, lastExpectedSan = result.expectedSan, statusMessage = null
                    )
                }

                viewModelScope.launch {
                    moveTrainer.advanceOpponentReplies()
                    if (moveTrainer.isSequenceComplete()) {
                        finishCurrentLine(success = true)
                    }
                    else {
                        _uiState.update {
                            it.copy(isWaitingForUserMove = true)
                        }
                    }
                }
            }

            is MoveTrainingEngine.MoveResult.Incorrect -> {
                _uiState.update {
                    it.copy(
                        lastMoveWasCorrect = false,
                        lastUserSan = result.userSan,
                        lastExpectedSan = result.expectedSan,
                        isWaitingForUserMove = true,
                        statusMessage = "Incorrect move"
                    )
                }
                chessController.navigateBack()
            }
        }
    }

    private suspend fun startLine(index: Int) {
        if (index !in lines.indices) {
            _uiState.update {
                it.copy(
                    isLoading = false, isSessionComplete = true, isWaitingForUserMove = false
                )
            }
            return
        }

        currentLineIndex = index
        val line = lines[index]

        val chapter = repertoireRepository.getChapterById(line.chapterId)
        val repertoire = chapter?.let { repertoireRepository.getRepertoireById(it.repertoireId) }
        val colorString = repertoire?.color ?: "White"

        mySide = if (colorString.equals("White", ignoreCase = true)) {
            Side.WHITE
        }
        else {
            Side.BLACK
        }

        currentLineMoves = repertoireRepository.getMovesForLine(line.id).first()
        moveTrainer.reset(
            MoveTrainingEngine.Config(
                mySide = mySide, sanMoves = currentLineMoves.map { it.moveSan })
        )

        chessController.resetBoard()
        chessController.allowedMoveSide = mySide

        if (mySide == Side.BLACK && !chessController.isFlipped) {
            chessController.flipBoard()
        }
        else if (mySide == Side.WHITE && chessController.isFlipped) {
            chessController.flipBoard()
        }

        _uiState.update {
            it.copy(
                isLoading = false,
                isSessionEmpty = false,
                isSessionComplete = false,
                currentLineName = line.name,
                currentLineNumber = index + 1,
                totalLines = lines.size,
                myColor = colorString,
                lastMoveWasCorrect = null,
                lastUserSan = null,
                lastExpectedSan = null,
                isWaitingForUserMove = false,
                statusMessage = null
            )
        }

        if (currentLineMoves.isEmpty()) {
            finishCurrentLine(success = true)
            return
        }

        // Auto-play any initial opponent moves before the first user move.
        moveTrainer.advanceOpponentReplies()

        if (moveTrainer.isSequenceComplete()) {
            finishCurrentLine(success = true)
        }
        else {
            _uiState.update {
                it.copy(isWaitingForUserMove = true)
            }
        }
    }

    private fun finishCurrentLine(success: Boolean) {
        viewModelScope.launch {
            val nextIndex = currentLineIndex + 1

            if (nextIndex >= lines.size) {
                _uiState.update {
                    it.copy(
                        isSessionComplete = true,
                        isWaitingForUserMove = false,
                        statusMessage = if (success) "Training complete" else "Training finished with mistakes"
                    )
                }
            }
            else {
                startLine(nextIndex)
            }
        }
    }

    class Factory(private val repertoireRepository: RepertoireRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val handle = extras.createSavedStateHandle()
            return TrainingViewModel(repertoireRepository, handle) as T
        }
    }
}



