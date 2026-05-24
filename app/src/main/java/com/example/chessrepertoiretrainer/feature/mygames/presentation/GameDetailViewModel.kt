package com.example.chessrepertoiretrainer.feature.mygames.presentation

import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.chessrepertoiretrainer.core.chess.controller.BoardAnnotations
import com.example.chessrepertoiretrainer.core.chess.controller.DefaultChessBoardController
import com.example.chessrepertoiretrainer.core.chess.domain.BoardMoveAnnotation
import com.example.chessrepertoiretrainer.core.chess.domain.findLegalMoveBySan
import com.example.chessrepertoiretrainer.core.chess.pgn.extract.PGNExtractor
import com.example.chessrepertoiretrainer.core.chess.pgn.navigator.LinearGameNavigator
import com.example.chessrepertoiretrainer.core.database.entity.SavedGame
import com.example.chessrepertoiretrainer.core.engine.EngineAnalysisHolder
import com.example.chessrepertoiretrainer.core.navigation.NavTransientStore
import com.example.chessrepertoiretrainer.core.engine.StockfishEngine
import com.example.chessrepertoiretrainer.core.database.entity.MoveEval
import com.example.chessrepertoiretrainer.feature.mygames.domain.OnDemandGameAnalyzer
import com.example.chessrepertoiretrainer.feature.mygames.domain.model.ComplianceStatus
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GameDetailViewModel(
    val game: SavedGame,
    private val analyzer: RepertoireComplianceAnalyzer,
    engine: StockfishEngine,
    private val gameAnalyzer: OnDemandGameAnalyzer? = null,
) : ViewModel() {

    val chessController = DefaultChessBoardController()
    val annotations = BoardAnnotations()
    val navigator = LinearGameNavigator()

    private val gameSanMoves: List<String> = PGNExtractor.extractSanMovesFromPgn(game.pgn)

    private val _annotations = MutableStateFlow<List<MoveAnnotation>>(emptyList())

    private val _isLoadingCompliance = MutableStateFlow(false)
    val isLoadingCompliance: StateFlow<Boolean> = _isLoadingCompliance.asStateFlow()

    private var cachedIndex: ComplianceIndex? = null

    val currentAnnotation: StateFlow<MoveAnnotation?> = combine(
        _annotations,
        snapshotFlow { navigator.currentMoveIndex }) { annotations, idx ->
        annotations.getOrNull(idx)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val engineHolder = EngineAnalysisHolder(engine, viewModelScope, chessController)
    val isEngineEnabled = engineHolder.isEnabled
    val engineAnalysis = engineHolder.analysis
    val engineSearchState = engineHolder.searchState
    val engineError = engineHolder.error

    private val _moveEvals = MutableStateFlow<List<MoveEval>>(emptyList())
    val moveEvals: StateFlow<List<MoveEval>> = _moveEvals.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _analysisProgress = MutableStateFlow(0 to 0)
    val analysisProgress: StateFlow<Pair<Int, Int>> = _analysisProgress.asStateFlow()

    init {
        chessController.isReadOnly = true
        chessController.onMoveApplied = { navigator.onUserMove(it) }
        navigator.onPositionChanged = { fen, lm -> chessController.loadPositionFromFen(fen, lm) }

        loadGame()
        viewModelScope.launch { _moveEvals.value = gameAnalyzer?.loadCachedEvals(game.id) ?: emptyList() }
        viewModelScope.launch { loadAnnotations() }
        viewModelScope.launch {
            combine(
                _annotations,
                _moveEvals,
                snapshotFlow { navigator.currentMoveIndex }
            ) { annots, evals, idx -> computeBadge(annots, evals, idx) }
                .collect { annotations.lastMoveAnnotation = it }
        }
    }

    private fun computeBadge(
        annots: List<MoveAnnotation>,
        evals: List<MoveEval>,
        idx: Int
    ): BoardMoveAnnotation? {
        if (idx < 0) return null
        val status = annots.getOrNull(idx)?.status
        if (status == ComplianceStatus.IN_BOOK || status == ComplianceStatus.OPPONENT_IN_BOOK) {
            return BoardMoveAnnotation.BOOK
        }
        return when (evals.getOrNull(idx)?.classification) {
            "BEST", "EXCELLENT" -> BoardMoveAnnotation.BEST
            "GOOD" -> BoardMoveAnnotation.GOOD
            "INACCURACY" -> BoardMoveAnnotation.INACCURACY
            "MISTAKE" -> BoardMoveAnnotation.MISTAKE
            "BLUNDER" -> BoardMoveAnnotation.BLUNDER
            else -> null
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

    fun toggleEngine() = engineHolder.toggle()
    fun analyzeDeeper() = engineHolder.analyzeDeeper()

    fun startAnalysis() {
        if (_isAnalyzing.value || gameAnalyzer == null) return
        viewModelScope.launch {
            _isAnalyzing.value = true
            try {
                val evals = gameAnalyzer.analyzeGame(game) { done, total ->
                    _analysisProgress.value = done to total
                }
                _moveEvals.value = evals ?: emptyList()
                if (!engineHolder.isEnabled.value) engineHolder.toggle()
            } finally {
                _isAnalyzing.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        engineHolder.dispose()
    }

    class Factory(
        private val store: NavTransientStore,
        private val analyzer: RepertoireComplianceAnalyzer,
        private val engine: StockfishEngine,
        private val gameAnalyzer: OnDemandGameAnalyzer? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val game = store.takeSavedGame()
                ?: error("GameDetailViewModel.Factory: no game in NavTransientStore")
            return GameDetailViewModel(game, analyzer, engine, gameAnalyzer) as T
        }
    }
}