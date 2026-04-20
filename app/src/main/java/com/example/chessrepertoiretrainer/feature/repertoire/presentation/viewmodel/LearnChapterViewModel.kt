package com.example.chessrepertoiretrainer.feature.repertoire.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.chessrepertoiretrainer.core.chess.controller.DefaultChessBoardController
import com.example.chessrepertoiretrainer.core.chess.domain.toSan
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

/**
 * ViewModel for the dedicated "learn" flow for a chapter.
 *
 * Responsibilities:
 *  - Load all lines for a chapter and step through them one by one.
 *  - For each line, let the user step through the moves from the starting
 *    position to the final position (no editing).
 *  - After the last move of a line, expose actions to either train that line
 *    (via a separate training screen) or skip training and go to the next line.
 *  - After all lines are processed, allow starting a final chapter-wide
 *    training session.
 */
class LearnChapterViewModel(
    private val repertoireRepository: RepertoireRepository, savedStateHandle: SavedStateHandle
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
        val currentMoveComment: String? = null
    )

    val chapterId: Int = checkNotNull(savedStateHandle["chapterId"])

    private val _uiState = MutableStateFlow(LearnChapterUiState(chapterId = chapterId))
    val uiState: StateFlow<LearnChapterUiState> = _uiState.asStateFlow()

    // Separate board controller for learn mode so we don't interfere with
    // the main TrainingViewModel's controller.
    val chessController = DefaultChessBoardController(onMoveListener = null)

    private var lines: List<Line> = emptyList()
    private var currentLineMoves: List<LineMove> = emptyList()
    private var currentLineIndex: Int = -1
    private var currentMoveIndex: Int = -1
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

        mySide = if (colorString.equals("White", ignoreCase = true)) {
            Side.WHITE
        }
        else {
            Side.BLACK
        }

        // Orient the board from the player's perspective once per session.
        if (mySide == Side.BLACK && !chessController.isFlipped) {
            chessController.flipBoard()
        }
        else if (mySide == Side.WHITE && chessController.isFlipped) {
            chessController.flipBoard()
        }

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

        // Prefer starting from the first unlearned line. If all lines are already
        // marked as learned, treat the chapter as complete.
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

    private suspend fun startLine(index: Int, chapterName: String? = null, colorString: String? = null) {
        if (index !in lines.indices) {
            // No more lines – chapter is complete.
            _uiState.update {
                it.copy(
                    isLoading = false, phase = LearnPhase.CHAPTER_COMPLETE, statusMessage = "You have gone through all lines in this chapter."
                )
            }
            return
        }

        currentLineIndex = index
        val line = lines[index]

        currentLineMoves = repertoireRepository.getMovesForLine(line.id).first()
        currentMoveIndex = -1

        chessController.resetBoard()
        // resetBoard() keeps isFlipped as-is, so orientation remains consistent.

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
                phase = if (currentLineMoves.isEmpty()) LearnPhase.LINE_COMPLETE else LearnPhase.STUDYING_LINE,
                isAtLineStart = true,
                isAtLineEnd = currentLineMoves.isEmpty(),
                statusMessage = if (currentLineMoves.isEmpty()) "This line has no moves." else null,
                currentMoveComment = null
            )
        }
    }

    /** Step forward by one move in the current line, if possible. */
    fun onNextMove() {
        val moves = currentLineMoves
        if (moves.isEmpty()) {
            // Nothing to step through – treat as completed line.
            _uiState.update {
                it.copy(phase = LearnPhase.LINE_COMPLETE, isAtLineEnd = true)
            }
            return
        }

        val nextIndex = currentMoveIndex + 1
        if (nextIndex !in moves.indices) {
            // Already at the end.
            _uiState.update {
                it.copy(phase = LearnPhase.LINE_COMPLETE, isAtLineEnd = true)
            }
            return
        }

        val targetSan = moves[nextIndex].moveSan
        val board = chessController.getBoard()
        val legalMove = board.legalMoves().firstOrNull { move ->
            board.toSan(move) == targetSan
        }

        if (legalMove == null) {
            _uiState.update {
                it.copy(statusMessage = "Cannot play move: $targetSan")
            }
            return
        }

        chessController.onMove(legalMove)
        currentMoveIndex = nextIndex

        val isEnd = currentMoveIndex >= moves.lastIndex
        val comment = moves.getOrNull(currentMoveIndex)?.comment?.takeIf { it.isNotBlank() }
        _uiState.update {
            it.copy(
                isAtLineStart = currentMoveIndex < 0,
                isAtLineEnd = isEnd,
                phase = if (isEnd) LearnPhase.LINE_COMPLETE else LearnPhase.STUDYING_LINE,
                statusMessage = null,
                currentMoveComment = comment
            )
        }
    }

    /** Step back by one move in the current line, if possible. */
    fun onPreviousMove() {
        val moves = currentLineMoves
        if (moves.isEmpty()) {
            return
        }

        if (currentMoveIndex < 0) {
            // Already at the beginning of the line.
            return
        }

        // Undo the last move on the board.
        chessController.navigateBack()
        currentMoveIndex--

        val isEnd = currentMoveIndex >= moves.lastIndex
        val comment = if (currentMoveIndex in moves.indices) {
            moves[currentMoveIndex].comment?.takeIf { it.isNotBlank() }
        }
        else {
            null
        }

        _uiState.update {
            it.copy(
                isAtLineStart = currentMoveIndex < 0,
                isAtLineEnd = isEnd,
                phase = if (isEnd) LearnPhase.LINE_COMPLETE else LearnPhase.STUDYING_LINE,
                statusMessage = null,
                currentMoveComment = comment
            )
        }
    }

    /** Restart the current line from the initial position. */
    fun restartCurrentLine() {
        if (currentLineIndex !in lines.indices) return

        chessController.resetBoard()
        currentMoveIndex = -1

        _uiState.update {
            it.copy(
                isAtLineStart = true,
                isAtLineEnd = currentLineMoves.isEmpty(),
                phase = if (currentLineMoves.isEmpty()) LearnPhase.LINE_COMPLETE else LearnPhase.STUDYING_LINE,
                statusMessage = null,
                currentMoveComment = null
            )
        }
    }

    /** Skip the training step for the current line and go to the next one. */
    fun skipTrainingForCurrentLine() {
        goToNextLine()
    }

    /** Called when the user finishes training this line in the movetrainer. */
    fun onLineTrainingFinished() {
        viewModelScope.launch {
            if (currentLineIndex in lines.indices) {
                val line = lines[currentLineIndex]
                val now = System.currentTimeMillis()
                val updated = if (!line.isLearned) {
                    line.copy(
                        isLearned = true, learnedAt = now, timesTrained = line.timesTrained + 1, lastTrainedAt = now
                    )
                }
                else {
                    line.copy(
                        timesTrained = line.timesTrained + 1, lastTrainedAt = now
                    )
                }
                repertoireRepository.updateLine(updated)
                // Keep local cache in sync so that subsequent navigation skips
                // learned lines correctly.
                lines = lines.toMutableList().also { list ->
                    list[currentLineIndex] = updated
                }
            }

            goToNextLine()
        }
    }

    private fun goToNextLine() {
        viewModelScope.launch {
            val nextIndex = (currentLineIndex + 1 until lines.size).firstOrNull { !lines[it].isLearned }

            if (nextIndex == null) {
                _uiState.update {
                    it.copy(
                        phase = LearnPhase.CHAPTER_COMPLETE, isAtLineEnd = true, statusMessage = "You have gone through all lines in this chapter."
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
            val savedStateHandle = extras.createSavedStateHandle()
            return LearnChapterViewModel(repertoireRepository, savedStateHandle) as T
        }
    }
}