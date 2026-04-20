package com.example.chessrepertoiretrainer.feature.repertoire.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.chessrepertoiretrainer.core.database.dao.RepertoireDao
import com.example.chessrepertoiretrainer.core.database.entity.Line
import com.example.chessrepertoiretrainer.core.database.entity.LineMove
import com.example.chessrepertoiretrainer.core.chess.controller.DefaultChessBoardController
import com.example.chessrepertoiretrainer.core.chess.domain.toSan
import com.github.bhlangonijr.chesslib.Side
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the simple "review" flow for a chapter.
 *
 * This is a read-only mode: you can step through the moves of each line and
 * move between lines quickly, but we do not modify any learning/progress
 * state. It is intended as a quick way to browse/check lines.
 */
class ReviewChapterViewModel(
    private val repertoireDao: RepertoireDao, savedStateHandle: SavedStateHandle
) : ViewModel() {

    data class UiState(
        val isLoading: Boolean = true,
        val hasNoLines: Boolean = false,
        val chapterId: Int = 0,
        val chapterName: String = "",
        val currentLineId: Int? = null,
        val currentLineName: String? = null,
        val currentLineNumber: Int = 0,
        val totalLines: Int = 0,
        val myColor: String? = null,
        val isAtLineStart: Boolean = true,
        val isAtLineEnd: Boolean = false,
        val statusMessage: String? = null,
        val currentMoveComment: String? = null
    )

    val chapterId: Int = checkNotNull(savedStateHandle["chapterId"])

    private val _uiState = MutableStateFlow(UiState(chapterId = chapterId))
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

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

        val chapter = repertoireDao.getChapterById(chapterId)
        val chapterName = chapter?.name ?: "Chapter"
        val repertoire = chapter?.let { repertoireDao.getRepertoireById(it.repertoireId) }
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

        val loadedLines = repertoireDao.getLinesForChapter(chapterId).first()
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

        startLine(0, chapterName = chapterName, colorString = colorString)
    }

    private suspend fun startLine(index: Int, chapterName: String? = null, colorString: String? = null) {
        if (index !in lines.indices) {
            // Out of range – nothing to show.
            _uiState.update {
                it.copy(
                    isLoading = false, statusMessage = "No more lines in this chapter."
                )
            }
            return
        }

        currentLineIndex = index
        val line = lines[index]

        currentLineMoves = repertoireDao.getMovesForLine(line.id).first()
        currentMoveIndex = -1

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
            _uiState.update { it.copy(isAtLineEnd = true) }
            return
        }

        val nextIndex = currentMoveIndex + 1
        if (nextIndex !in moves.indices) {
            _uiState.update { it.copy(isAtLineEnd = true) }
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
                isAtLineStart = currentMoveIndex < 0, isAtLineEnd = isEnd, statusMessage = null, currentMoveComment = comment
            )
        }
    }

    /** Step back by one move in the current line, if possible. */
    fun onPreviousMove() {
        val moves = currentLineMoves
        if (moves.isEmpty()) return
        if (currentMoveIndex < 0) return

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
                isAtLineStart = currentMoveIndex < 0, isAtLineEnd = isEnd, statusMessage = null, currentMoveComment = comment
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
                isAtLineStart = true, isAtLineEnd = currentLineMoves.isEmpty(), statusMessage = null, currentMoveComment = null
            )
        }
    }

    /** Go to the previous line in the chapter, if any. */
    fun goToPreviousLine() {
        viewModelScope.launch {
            val prevIndex = currentLineIndex - 1
            if (prevIndex in lines.indices) {
                startLine(prevIndex)
            }
        }
    }

    /** Go to the next line in the chapter, if any. */
    fun goToNextLine() {
        viewModelScope.launch {
            val nextIndex = currentLineIndex + 1
            if (nextIndex in lines.indices) {
                startLine(nextIndex)
            }
        }
    }

    class Factory(private val repertoireDao: RepertoireDao) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val savedStateHandle = extras.createSavedStateHandle()
            return ReviewChapterViewModel(repertoireDao, savedStateHandle) as T
        }
    }
}