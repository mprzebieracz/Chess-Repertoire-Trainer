package com.example.chessrepertoiretrainer.feature.repertoire.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.chessrepertoiretrainer.core.chess.controller.BoardAnnotations
import com.example.chessrepertoiretrainer.core.chess.controller.DefaultChessBoardController
import com.example.chessrepertoiretrainer.core.chess.domain.findLegalMoveBySan
import com.example.chessrepertoiretrainer.core.chess.domain.toSide
import com.example.chessrepertoiretrainer.core.chess.pgn.navigator.GuidedLineNavigator
import com.example.chessrepertoiretrainer.core.database.entity.Line
import com.example.chessrepertoiretrainer.core.engine.EngineAnalysisHolder
import com.example.chessrepertoiretrainer.core.engine.StockfishEngine
import com.example.chessrepertoiretrainer.feature.repertoire.domain.RepertoireRepository
import com.github.bhlangonijr.chesslib.Side
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


class ReviewChapterViewModel(
    private val repertoireRepository: RepertoireRepository,
    savedStateHandle: SavedStateHandle,
    engine: StockfishEngine,
) : ViewModel() {

    data class ReviewChapterUiState(
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
        val currentMoveLabel: String? = null,  // e.g. "3. f4" or "3… f4"
        val currentMoveComment: String? = null,
    )

    val chapterId: Int = checkNotNull(savedStateHandle["chapterId"])
    private val startLineId: Int? = savedStateHandle.get<Int>("startLineId")?.takeIf { it != -1 }

    private val _uiState = MutableStateFlow(ReviewChapterUiState(chapterId = chapterId))
    val uiState: StateFlow<ReviewChapterUiState> = _uiState.asStateFlow()

    val chessController = DefaultChessBoardController()
    val annotations = BoardAnnotations()

    private var lines: List<Line> = emptyList()
    private var lineNavigator: GuidedLineNavigator = GuidedLineNavigator.empty()
    private var currentLineIndex: Int = -1
    private var mySide: Side = Side.WHITE

    private val engineHolder = EngineAnalysisHolder(engine, viewModelScope, chessController)
    val isEngineEnabled = engineHolder.isEnabled
    val engineAnalysis = engineHolder.analysis
    val engineSearchState = engineHolder.searchState
    val engineError = engineHolder.error

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

        val initialIndex =
            startLineId?.let { id -> loadedLines.indexOfFirst { it.id == id }.takeIf { it >= 0 } }
                ?: 0
        startLine(initialIndex, chapterName = chapterName, colorString = colorString)
    }

    private suspend fun startLine(
        index: Int,
        chapterName: String? = null,
        colorString: String? = null
    ) {
        if (index !in lines.indices) {
            _uiState.update {
                it.copy(isLoading = false, statusMessage = "No more lines in this chapter.")
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
            _uiState.update { it.copy(isAtLineEnd = true) }
            return
        }

        val legalMove = chessController.getBoard().findLegalMoveBySan(nextSan)
        if (legalMove == null) {
            _uiState.update { it.copy(statusMessage = "Cannot play move: $nextSan") }
            return
        }

        // Suppress onMoveApplied so navigator doesn't receive this as a user move.
        val saved = chessController.onMoveApplied
        chessController.onMoveApplied = null
        chessController.onMove(legalMove)
        chessController.onMoveApplied = saved

        lineNavigator.goNext()

        _uiState.update {
            it.copy(
                isAtLineStart = lineNavigator.isAtStart,
                isAtLineEnd = lineNavigator.isAtEnd,
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
                statusMessage = null,
                currentMoveLabel = null,
                currentMoveComment = null,
            )
        }
    }

    fun goToPreviousLine() {
        viewModelScope.launch {
            val prevIndex = currentLineIndex - 1
            if (prevIndex in lines.indices) startLine(prevIndex)
        }
    }

    fun goToNextLine() {
        viewModelScope.launch {
            val nextIndex = currentLineIndex + 1
            if (nextIndex in lines.indices) startLine(nextIndex)
        }
    }

    fun toggleEngine() = engineHolder.toggle()
    fun analyzeDeeper() = engineHolder.analyzeDeeper()

    override fun onCleared() {
        super.onCleared()
        engineHolder.dispose()
    }

    /** e.g. "3. f4" (white) or "3… f4" (black). Null when at root position. */
    private fun moveLabel(): String? {
        val node = lineNavigator.currentNode() ?: return null
        val fenParts = node.fenBefore.split(" ")
        val moveNum = fenParts.getOrNull(5)?.toIntOrNull() ?: 1
        val isWhite = fenParts.getOrNull(1) != "b"
        return if (isWhite) "$moveNum. ${node.san}" else "$moveNum… ${node.san}"
    }

    class Factory(
        private val repertoireRepository: RepertoireRepository,
        private val engine: StockfishEngine
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val savedStateHandle = extras.createSavedStateHandle()
            return ReviewChapterViewModel(repertoireRepository, savedStateHandle, engine) as T
        }
    }
}
