package com.example.chessrepertoiretrainer.feature.repertoire.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.chessrepertoiretrainer.core.chess.controller.DefaultChessBoardController
import com.example.chessrepertoiretrainer.core.chess.domain.findLegalMoveBySan
import com.example.chessrepertoiretrainer.core.chess.domain.toSide
import com.example.chessrepertoiretrainer.core.chess.pgn.navigator.GuidedLineNavigator
import com.example.chessrepertoiretrainer.core.database.entity.Line
import com.example.chessrepertoiretrainer.feature.repertoire.domain.RepertoireRepository
import com.github.bhlangonijr.chesslib.Side
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


class LearnChapterViewModel(
    private val repertoireRepository: RepertoireRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    enum class LearnPhase {
        STUDYING_LINE, LINE_COMPLETE, CHAPTER_COMPLETE
    }

    data class LearnChapterUiState(
        val isLoading: Boolean = true,
        val hasNoLines: Boolean = false,
        val chapterId: Int = 0,
        val chapterName: String = "",
        val currentLineId: Int? = null,
        val currentLineName: String? = null,
        val currentLineNumber: Int = 0,
        val totalLines: Int = 0,
        val myColor: String? = null,
        val phase: LearnPhase = LearnPhase.STUDYING_LINE,
        val isAtLineStart: Boolean = true,
        val isAtLineEnd: Boolean = false,
        val statusMessage: String? = null,
        val currentMoveLabel: String? = null,
        val currentMoveComment: String? = null,
    )

    val chapterId: Int = checkNotNull(savedStateHandle["chapterId"])

    private val _uiState = MutableStateFlow(LearnChapterUiState(chapterId = chapterId))
    val uiState: StateFlow<LearnChapterUiState> = _uiState.asStateFlow()

    // Separate board controller for learn mode so we don't interfere with
    // the main TrainingViewModel's controller.
    val chessController = DefaultChessBoardController()

    private var lines: List<Line> = emptyList()
    private var lineNavigator: GuidedLineNavigator = GuidedLineNavigator.empty()
    private var currentLineIndex: Int = -1
    private var mySide: Side = Side.WHITE

    init {
        viewModelScope.launch {
            loadChapterAndLines()
        }
    }

    private suspend fun loadChapterAndLines() {
        _uiState.update { it.copy(isLoading = true, hasNoLines = false, statusMessage = null) }

        val chapter = repertoireRepository.getChapterById(chapterId)
        val chapterName = chapter?.name ?: "Chapter"
        val repertoire = chapter?.let { repertoireRepository.getRepertoireById(it.repertoireId) }
        val colorString = repertoire?.color ?: "White"

        mySide = colorString.toSide()

        // Orient the board from the player's perspective once per session.
        chessController.orientForSide(mySide)

        val loadedLines = repertoireRepository.getLinesForChapter(chapterId).first()
        lines = loadedLines

        if (loadedLines.isEmpty()) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    hasNoLines = true,
                    chapterName = chapterName,
                    totalLines = 0,
                    statusMessage = "No lines in this chapter yet. Use edit mode to add lines."
                )
            }
            return
        }

        val firstUnlearnedIndex = loadedLines.indexOfFirst { !it.isLearned }
        if (firstUnlearnedIndex == -1) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    hasNoLines = false,
                    chapterName = chapterName,
                    totalLines = loadedLines.size,
                    phase = LearnPhase.CHAPTER_COMPLETE,
                    statusMessage = "All lines in this chapter are already learned."
                )
            }
            return
        }

        startLine(firstUnlearnedIndex, chapterName = chapterName, colorString = colorString)
    }

    private suspend fun startLine(
        index: Int,
        chapterName: String? = null,
        colorString: String? = null
    ) {
        if (index !in lines.indices) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    phase = LearnPhase.CHAPTER_COMPLETE,
                    statusMessage = "You have gone through all lines in this chapter."
                )
            }
            return
        }

        currentLineIndex = index
        val line = lines[index]

        val loadedMoves = repertoireRepository.getMovesForLine(line.id).first()
        lineNavigator = GuidedLineNavigator(loadedMoves)
        chessController.resetBoard()

        _uiState.update {
            it.copy(
                isLoading = false,
                hasNoLines = false,
                chapterName = chapterName ?: it.chapterName,
                currentLineId = line.id,
                currentLineName = line.name,
                currentLineNumber = index + 1,
                totalLines = lines.size,
                myColor = colorString ?: it.myColor,
                phase = if (lineNavigator.isEmpty) LearnPhase.LINE_COMPLETE else LearnPhase.STUDYING_LINE,
                isAtLineStart = true,
                isAtLineEnd = lineNavigator.isEmpty,
                statusMessage = if (lineNavigator.isEmpty) "This line has no moves." else null,
                currentMoveLabel = null,
                currentMoveComment = null,
            )
        }
    }

    fun onNextMove() {
        val nextSan = lineNavigator.peekNextSan() ?: run {
            _uiState.update { it.copy(phase = LearnPhase.LINE_COMPLETE, isAtLineEnd = true) }
            return
        }

        val legalMove = chessController.getBoard().findLegalMoveBySan(nextSan)
        if (legalMove == null) {
            _uiState.update { it.copy(statusMessage = "Cannot play move: $nextSan") }
            return
        }

        val saved = chessController.onMoveApplied
        chessController.onMoveApplied = null
        chessController.onMove(legalMove)
        chessController.onMoveApplied = saved

        lineNavigator.goNext()

        _uiState.update {
            it.copy(
                isAtLineStart = lineNavigator.isAtStart,
                isAtLineEnd = lineNavigator.isAtEnd,
                phase = if (lineNavigator.isAtEnd) LearnPhase.LINE_COMPLETE else LearnPhase.STUDYING_LINE,
                statusMessage = null,
                currentMoveLabel = moveLabel(),
                currentMoveComment = lineNavigator.currentComment,
            )
        }
    }

    fun onPreviousMove() {
        if (lineNavigator.isAtStart) return
        lineNavigator.goPrevious()
        chessController.loadPositionFromFen(lineNavigator.currentFen())
        _uiState.update {
            it.copy(
                isAtLineStart = lineNavigator.isAtStart,
                isAtLineEnd = lineNavigator.isAtEnd,
                phase = if (lineNavigator.isAtEnd) LearnPhase.LINE_COMPLETE else LearnPhase.STUDYING_LINE,
                statusMessage = null,
                currentMoveLabel = moveLabel(),
                currentMoveComment = lineNavigator.currentComment,
            )
        }
    }

    fun restartCurrentLine() {
        if (currentLineIndex !in lines.indices) return
        chessController.resetBoard()
        lineNavigator.reset()
        _uiState.update {
            it.copy(
                isAtLineStart = true,
                isAtLineEnd = lineNavigator.isEmpty,
                phase = if (lineNavigator.isEmpty) LearnPhase.LINE_COMPLETE else LearnPhase.STUDYING_LINE,
                statusMessage = null,
                currentMoveLabel = null,
                currentMoveComment = null,
            )
        }
    }

    private fun moveLabel(): String? {
        val node = lineNavigator.currentNode() ?: return null
        val fenParts = node.fenBefore.split(" ")
        val moveNum = fenParts.getOrNull(5)?.toIntOrNull() ?: 1
        val isWhite = fenParts.getOrNull(1) != "b"
        return if (isWhite) "$moveNum. ${node.san}" else "$moveNum… ${node.san}"
    }

    fun skipTrainingForCurrentLine() {
        goToNextLine()
    }

    fun onLineTrainingFinished() {
        viewModelScope.launch {
            if (currentLineIndex in lines.indices) {
                val line = lines[currentLineIndex]
                val now = System.currentTimeMillis()
                val updated = if (!line.isLearned) {
                    line.copy(
                        isLearned = true,
                        learnedAt = now,
                        timesTrained = line.timesTrained + 1,
                        lastTrainedAt = now
                    )
                } else {
                    line.copy(timesTrained = line.timesTrained + 1, lastTrainedAt = now)
                }
                repertoireRepository.updateLine(updated)
                lines = lines.toMutableList().also { list ->
                    list[currentLineIndex] = updated
                }
            }

            goToNextLine()
        }
    }

    private fun goToNextLine() {
        viewModelScope.launch {
            val nextIndex =
                (currentLineIndex + 1 until lines.size).firstOrNull { !lines[it].isLearned }

            if (nextIndex == null) {
                _uiState.update {
                    it.copy(
                        phase = LearnPhase.CHAPTER_COMPLETE,
                        isAtLineEnd = true,
                        statusMessage = "You have gone through all lines in this chapter."
                    )
                }
            } else {
                startLine(nextIndex)
            }
        }
    }

    class Factory(private val repertoireRepository: RepertoireRepository) :
        ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val savedStateHandle = extras.createSavedStateHandle()
            return LearnChapterViewModel(repertoireRepository, savedStateHandle) as T
        }
    }
}