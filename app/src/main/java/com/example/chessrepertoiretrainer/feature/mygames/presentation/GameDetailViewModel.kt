package com.example.chessrepertoiretrainer.feature.mygames.presentation

import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.chessrepertoiretrainer.core.chess.controller.DefaultChessBoardController
import com.example.chessrepertoiretrainer.core.chess.domain.findLegalMoveBySan
import com.example.chessrepertoiretrainer.core.chess.pgn.extract.PGNExtractor
import com.example.chessrepertoiretrainer.core.chess.pgn.navigator.LinearGameNavigator
import com.example.chessrepertoiretrainer.core.database.entity.SavedGame
import com.example.chessrepertoiretrainer.core.engine.EngineAnalysis
import com.example.chessrepertoiretrainer.core.engine.EngineSearchState
import com.example.chessrepertoiretrainer.core.engine.StockfishEngine
import com.example.chessrepertoiretrainer.feature.mygames.domain.model.MoveAnnotation
import com.example.chessrepertoiretrainer.feature.repertoire.domain.usecase.ComplianceIndex
import com.example.chessrepertoiretrainer.feature.repertoire.domain.usecase.RepertoireComplianceAnalyzer
import com.github.bhlangonijr.chesslib.Side
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GameDetailViewModel(
    val game: SavedGame,
    private val analyzer: RepertoireComplianceAnalyzer,
    private val engine: StockfishEngine
) : ViewModel() {

    val chessController = DefaultChessBoardController()
    val navigator = LinearGameNavigator()

    private val gameSanMoves: List<String> = PGNExtractor.extractSanMovesFromPgn(game.pgn)

    private val _complianceEnabled = MutableStateFlow(false)
    val complianceEnabled: StateFlow<Boolean> = _complianceEnabled.asStateFlow()

    private val _annotations = MutableStateFlow<List<MoveAnnotation>>(emptyList())

    private val _isLoadingCompliance = MutableStateFlow(false)
    val isLoadingCompliance: StateFlow<Boolean> = _isLoadingCompliance.asStateFlow()

    private var cachedIndex: ComplianceIndex? = null

    val currentAnnotation: StateFlow<MoveAnnotation?> = combine(
        _annotations,
        snapshotFlow { navigator.currentMoveIndex }) { annotations, idx ->
        annotations.getOrNull(idx)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

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
        chessController.onMoveApplied = { navigator.onUserMove(it) }
        navigator.onPositionChanged = { fen, lm -> chessController.loadPositionFromFen(fen, lm) }

        loadGame()
        viewModelScope.launch {
            snapshotFlow { chessController.boardState }.distinctUntilChanged().collect { fen ->
                if (engine.isEnabled.value) engine.updatePosition(fen)
            }
        }
    }

    private fun loadGame() {
        chessController.resetBoard()
        val board = chessController.getBoard()
        // Seed the navigator by replaying all moves through the controller
        // (onMoveApplied fires for each, populating the navigator).
        for (san in gameSanMoves) {
            val move = board.findLegalMoveBySan(san) ?: break
            chessController.tryApplyMove(move)
        }
        // Go back to start — navigator handles the position push.
        navigator.reset()
        chessController.orientForSide(if (game.isPlayerWhite) Side.WHITE else Side.BLACK)
    }

    fun toggleCompliance() {
        val enabling = !_complianceEnabled.value
        _complianceEnabled.value = enabling
        if (enabling) {
            viewModelScope.launch { loadAnnotations() }
        } else {
            _annotations.value = emptyList()
        }
    }

    fun rebuildIndex() {
        viewModelScope.launch {
            _isLoadingCompliance.value = true
            try {
                val index = analyzer.rebuildIndex(game.isPlayerWhite)
                cachedIndex = index
                _annotations.value = withContext(Dispatchers.Default) {
                    analyzer.annotate(gameSanMoves, game.isPlayerWhite, index)
                }
            } finally {
                _isLoadingCompliance.value = false
            }
        }
    }

    private suspend fun loadAnnotations() {
        _isLoadingCompliance.value = true
        try {
            val index =
                cachedIndex ?: analyzer.buildIndex(game.isPlayerWhite).also { cachedIndex = it }
            _annotations.value = withContext(Dispatchers.Default) {
                analyzer.annotate(gameSanMoves, game.isPlayerWhite, index)
            }
        } finally {
            _isLoadingCompliance.value = false
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
        private val game: SavedGame,
        private val analyzer: RepertoireComplianceAnalyzer,
        private val engine: StockfishEngine
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            return GameDetailViewModel(game, analyzer, engine) as T
        }
    }
}