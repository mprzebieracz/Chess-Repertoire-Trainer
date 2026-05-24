package com.example.chessrepertoiretrainer.feature.repertoire.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.chessrepertoiretrainer.app.AppContainer
import com.example.chessrepertoiretrainer.core.chess.controller.DefaultChessBoardController
import com.example.chessrepertoiretrainer.core.chess.domain.toSide
import com.example.chessrepertoiretrainer.core.chess.training.MoveTrainingEngine
import com.example.chessrepertoiretrainer.core.database.entity.Line
import com.example.chessrepertoiretrainer.core.database.entity.LineMove
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
    val currentChapterName: String? = null,
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
    private val repertoireRepository: RepertoireRepository,
    savedStateHandle: SavedStateHandle,
    private val multiChapterIds: List<Int>? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrainingUiState())
    val uiState: StateFlow<TrainingUiState> = _uiState.asStateFlow()

    val chessController = DefaultChessBoardController()
    private val moveTrainer =
        MoveTrainingEngine(chessController, viewModelScope) { normalizeSan(it) }

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
        moveTrainer.setMoveResultListener(::handleMoveResult)

        viewModelScope.launch {
            loadTrainingSession()
        }
    }

    private suspend fun loadTrainingSession() {
        if (lineId != null) {
            loadSingleLineSession(lineId)
            return
        }

        if (multiChapterIds != null) {
            loadMultiChapterSession(multiChapterIds)
            return
        }

        observeSessionLines()
    }

    private suspend fun loadMultiChapterSession(chapterIds: List<Int>) {
        val allLines = repertoireRepository.getLinesForChapters(chapterIds)
        if (allLines.isEmpty()) {
            showEmptySession()
            return
        }
        lines = allLines.shuffled()
        startLine(0)
    }

    private suspend fun loadSingleLineSession(lineId: Int) {
        val line = repertoireRepository.getLineById(lineId)

        if (line == null) {
            showEmptySession("Line not found")
            return
        }

        lines = listOf(line)
        startLine(0)
    }

    private suspend fun observeSessionLines() {
        val flow = if (chapterId != null) {
            repertoireRepository.getLinesForChapter(chapterId)
        } else {
            val allLinesTime = Long.MAX_VALUE
            repertoireRepository.getLinesToReview(allLinesTime)
        }

        flow.collect(::handleLoadedLines)
    }

    private suspend fun handleLoadedLines(loadedLines: List<Line>) {
        if (loadedLines.isEmpty()) {
            showEmptySession()
            return
        }

        if (isFirstSessionLoad()) {
            lines = prepareSessionLines(loadedLines)
            startLine(0)
        } else {
            updateLoadedSessionSize()
        }
    }

    private fun isFirstSessionLoad(): Boolean = currentLineIndex == -1

    private fun prepareSessionLines(loadedLines: List<Line>): List<Line> {
        // Chapter-based training should feel less repetitive; review mode keeps DAO order.
        return if (chapterId != null || multiChapterIds != null) loadedLines.shuffled() else loadedLines
    }

    private fun updateLoadedSessionSize() {
        _uiState.update { state ->
            state.copy(totalLines = lines.size)
        }
    }

    private fun showEmptySession(statusMessage: String = "") {
        _uiState.update {
            it.copy(
                isLoading = false,
                isSessionEmpty = true,
                isSessionComplete = false,
                totalLines = 0,
                currentLineName = null,
                statusMessage = statusMessage.ifBlank { null })
        }
    }

    private fun handleMoveResult(result: MoveTrainingEngine.MoveResult) {
        when (result) {
            is MoveTrainingEngine.MoveResult.Correct -> {
                _uiState.update {
                    it.copy(
                        lastMoveWasCorrect = true,
                        lastUserSan = result.userSan,
                        lastExpectedSan = result.expectedSan,
                        isWaitingForUserMove = !result.isComplete,
                        statusMessage = null,
                    )
                }
                if (result.isComplete) viewModelScope.launch { finishCurrentLine() }
            }

            is MoveTrainingEngine.MoveResult.Incorrect -> {
                _uiState.update {
                    it.copy(
                        lastMoveWasCorrect = false,
                        lastUserSan = result.userSan,
                        lastExpectedSan = result.expectedSan,
                        isWaitingForUserMove = true,
                        statusMessage = "Incorrect move",
                    )
                }
            }
        }
    }

    private suspend fun startLine(index: Int) {
        if (index !in lines.indices) {
            showSessionComplete()
            return
        }

        currentLineIndex = index
        val line = lines[index]

        val lineContext = loadLineContext(line)
        currentLineMoves = lineContext.moves
        mySide = lineContext.side

        configureMoveTrainer()
        configureBoardForCurrentSide()
        updateLineUi(
            line = line,
            colorString = lineContext.colorName,
            index = index,
            chapterName = lineContext.chapterName
        )

        if (currentLineMoves.isEmpty()) {
            finishCurrentLine()
            return
        }

        autoPlayOpeningReplies()
    }

    private suspend fun loadLineContext(line: Line): LineContext {
        val chapter = repertoireRepository.getChapterById(line.chapterId)
        val repertoire = chapter?.let { repertoireRepository.getRepertoireById(it.repertoireId) }
        val colorString = repertoire?.color ?: "White"
        val side = colorString.toSide()
        val moves = repertoireRepository.getMovesForLine(line.id).first()

        return LineContext(
            colorName = colorString,
            side = side,
            moves = moves,
            chapterName = chapter?.name
        )
    }

    private fun configureMoveTrainer() {
        moveTrainer.reset(
            MoveTrainingEngine.Config(
                mySide = mySide,
                sanMoves = currentLineMoves.map { it.moveSan })
        )
    }

    private fun configureBoardForCurrentSide() {
        chessController.resetBoard()
        chessController.allowedMoveSide = mySide
        chessController.orientForSide(mySide)
    }

    private fun updateLineUi(line: Line, colorString: String, index: Int, chapterName: String?) {
        _uiState.update {
            it.copy(
                isLoading = false,
                isSessionEmpty = false,
                isSessionComplete = false,
                currentLineName = line.name,
                currentChapterName = chapterName,
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
    }

    fun showHint() {
        val sq = moveTrainer.computeHintSquare() ?: return
        chessController.markedSquare = sq
    }

    fun showSolution() {
        viewModelScope.launch { moveTrainer.playSolutionStep() }
    }

    private suspend fun autoPlayOpeningReplies() {
        moveTrainer.advanceOpponentReplies()

        if (moveTrainer.isSequenceComplete()) {
            finishCurrentLine()
        } else {
            _uiState.update {
                it.copy(isWaitingForUserMove = true)
            }
        }
    }

    private fun showSessionComplete() {
        _uiState.update {
            it.copy(isLoading = false, isSessionComplete = true, isWaitingForUserMove = false)
        }
    }

    private suspend fun finishCurrentLine() {
        val nextIndex = currentLineIndex + 1

        if (nextIndex >= lines.size) {
            _uiState.update {
                it.copy(
                    isSessionComplete = true,
                    isWaitingForUserMove = false,
                    statusMessage = "Training complete"
                )
            }
        } else {
            startLine(nextIndex)
        }
    }

    private data class LineContext(
        val colorName: String,
        val side: Side,
        val moves: List<LineMove>,
        val chapterName: String?
    )

    class Factory(
        private val repertoireRepository: RepertoireRepository,
        private val appContainer: AppContainer? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val handle = extras.createSavedStateHandle()
            return TrainingViewModel(
                repertoireRepository,
                handle,
                appContainer?.navTransientStore?.takeSelectedChapterIds()
            ) as T
        }
    }
}