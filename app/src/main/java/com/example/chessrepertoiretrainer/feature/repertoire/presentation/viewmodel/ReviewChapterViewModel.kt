package com.example.chessrepertoiretrainer.feature.repertoire.presentation.viewmodel

import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.chessrepertoiretrainer.core.chess.controller.DefaultChessBoardController
import com.example.chessrepertoiretrainer.core.chess.domain.findLegalMoveBySan
import com.example.chessrepertoiretrainer.core.chess.domain.toSide
import com.example.chessrepertoiretrainer.core.chess.pgn.LinearPgnLineSource
import com.example.chessrepertoiretrainer.core.database.entity.Line
import com.example.chessrepertoiretrainer.core.engine.EngineAnalysis
import com.example.chessrepertoiretrainer.core.engine.EngineSearchState
import com.example.chessrepertoiretrainer.core.engine.StockfishEngine
import com.example.chessrepertoiretrainer.feature.repertoire.domain.RepertoireRepository
import com.github.bhlangonijr.chesslib.Side
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


class ReviewChapterViewModel(
    private val repertoireRepository: RepertoireRepository,
    savedStateHandle: SavedStateHandle,
    private val engine: StockfishEngine
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
        val currentMoveComment: String? = null
    )

    val chapterId: Int = checkNotNull(savedStateHandle["chapterId"])
    private val startLineId: Int? = savedStateHandle.get<Int>("startLineId")?.takeIf { it != -1 }

    private val _uiState = MutableStateFlow(ReviewChapterUiState(chapterId = chapterId))
    val uiState: StateFlow<ReviewChapterUiState> = _uiState.asStateFlow()

    val chessController = DefaultChessBoardController(onMoveListener = null)

    private var lines: List<Line> = emptyList()
    private var lineSource: LinearPgnLineSource = LinearPgnLineSource(emptyList())
    private var currentLineIndex: Int = -1
    private var mySide: Side = Side.WHITE

    val isEngineEnabled: StateFlow<Boolean> =
        engine.isEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    val engineAnalysis: StateFlow<EngineAnalysis?> =
        engine.analysis.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val engineSearchState: StateFlow<EngineSearchState> = engine.searchState.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(
            5_000
        ),
        EngineSearchState.IDLE
    )
    val engineError: StateFlow<String?> =
        engine.engineError.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        viewModelScope.launch {
            loadChapterAndLines()
        }
        viewModelScope.launch {
            snapshotFlow { chessController.boardState }.distinctUntilChanged().collect { fen ->
                if (engine.isEnabled.value) engine.updatePosition(fen)
            }
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
            // Out of range – nothing to show.
            _uiState.update {
                it.copy(isLoading = false, statusMessage = "No more lines in this chapter.")
            }
            return
        }

        currentLineIndex = index
        val line = lines[index]

        val loadedMoves = repertoireRepository.getMovesForLine(line.id).first()
        lineSource = LinearPgnLineSource(loadedMoves)

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
                isAtLineEnd = lineSource.isEmpty,
                statusMessage = if (lineSource.isEmpty) "This line has no moves." else null,
                currentMoveComment = null
            )
        }
    }

    fun onNextMove() {
        if (lineSource.isEmpty) {
            _uiState.update { it.copy(isAtLineEnd = true) }
            return
        }

        val moveData = lineSource.next() ?: run {
            _uiState.update { it.copy(isAtLineEnd = true) }
            return
        }

        val legalMove = chessController.getBoard().findLegalMoveBySan(moveData.san)
        if (legalMove == null) {
            lineSource.previous()
            _uiState.update { it.copy(statusMessage = "Cannot play move: ${moveData.san}") }
            return
        }

        chessController.onMove(legalMove)
        _uiState.update {
            it.copy(
                isAtLineStart = lineSource.isAtStart,
                isAtLineEnd = lineSource.isAtEnd,
                statusMessage = null,
                currentMoveComment = lineSource.currentComment
            )
        }
    }

    fun onPreviousMove() {
        if (lineSource.isAtStart) return

        lineSource.previous()
        chessController.navigateBack()

        _uiState.update {
            it.copy(
                isAtLineStart = lineSource.isAtStart,
                isAtLineEnd = lineSource.isAtEnd,
                statusMessage = null,
                currentMoveComment = lineSource.currentComment
            )
        }
    }

    fun restartCurrentLine() {
        if (currentLineIndex !in lines.indices) return

        chessController.resetBoard()
        lineSource.reset()

        _uiState.update {
            it.copy(
                isAtLineStart = true,
                isAtLineEnd = lineSource.isEmpty,
                statusMessage = null,
                currentMoveComment = null
            )
        }
    }

    fun goToPreviousLine() {
        viewModelScope.launch {
            val prevIndex = currentLineIndex - 1
            if (prevIndex in lines.indices) {
                startLine(prevIndex)
            }
        }
    }

    fun goToNextLine() {
        viewModelScope.launch {
            val nextIndex = currentLineIndex + 1
            if (nextIndex in lines.indices) {
                startLine(nextIndex)
            }
        }
    }

    fun toggleEngine() {
        if (engine.isEnabled.value) engine.disable()
        else engine.enable(chessController.boardState)
    }

    fun analyzeDeeper() = engine.analyzeDeeper()

    override fun onCleared() {
        super.onCleared()
        engine.disable()
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