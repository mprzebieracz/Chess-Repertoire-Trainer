package com.example.chessrepertoiretrainer.feature.mygames.presentation

import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.chessrepertoiretrainer.core.chess.controller.DefaultChessBoardController
import com.example.chessrepertoiretrainer.core.chess.domain.moveFromSan
import com.example.chessrepertoiretrainer.core.chess.utils.PGNExtractor
import com.example.chessrepertoiretrainer.core.database.entity.SavedGame
import com.example.chessrepertoiretrainer.feature.mygames.domain.model.MoveAnnotation
import com.example.chessrepertoiretrainer.feature.mygames.domain.usecase.ComplianceIndex
import com.example.chessrepertoiretrainer.feature.mygames.domain.usecase.RepertoireComplianceAnalyzer
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
    private val analyzer: RepertoireComplianceAnalyzer
) : ViewModel() {

    val chessController = DefaultChessBoardController()

    private val gameSanMoves: List<String>

    private val _complianceEnabled = MutableStateFlow(false)
    val complianceEnabled: StateFlow<Boolean> = _complianceEnabled.asStateFlow()

    private val _annotations = MutableStateFlow<List<MoveAnnotation>>(emptyList())

    private val _isLoadingCompliance = MutableStateFlow(false)
    val isLoadingCompliance: StateFlow<Boolean> = _isLoadingCompliance.asStateFlow()

    private var cachedIndex: ComplianceIndex? = null

    val currentAnnotation: StateFlow<MoveAnnotation?> = combine(
        _annotations,
        snapshotFlow { chessController.currentMoveIndex }
    ) { annotations, idx ->
        annotations.getOrNull(idx)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        gameSanMoves = PGNExtractor.extractSanMovesFromPgn(game.pgn)
        loadGame()
    }

    private fun loadGame() {
        chessController.resetBoard()
        val board = chessController.getBoard()
        for (san in gameSanMoves) {
            val move = board.moveFromSan(san) ?: break
            chessController.onMove(move)
        }
        repeat(gameSanMoves.size) { chessController.navigateBack() }

        if (game.isPlayerWhite == chessController.isFlipped) {
            chessController.flipBoard()
        }
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
            val index = cachedIndex ?: analyzer.buildIndex(game.isPlayerWhite).also { cachedIndex = it }
            _annotations.value = withContext(Dispatchers.Default) {
                analyzer.annotate(gameSanMoves, game.isPlayerWhite, index)
            }
        } finally {
            _isLoadingCompliance.value = false
        }
    }

    class Factory(
        private val game: SavedGame,
        private val analyzer: RepertoireComplianceAnalyzer
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            return GameDetailViewModel(game, analyzer) as T
        }
    }
}
