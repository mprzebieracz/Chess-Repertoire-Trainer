package com.example.chessrepertoiretrainer.feature.analysis

import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.chessrepertoiretrainer.core.chess.controller.BoardAnnotations
import com.example.chessrepertoiretrainer.core.chess.controller.ChessBoardController
import com.example.chessrepertoiretrainer.core.chess.controller.DefaultChessBoardController
import com.example.chessrepertoiretrainer.core.chess.domain.Arrow
import com.example.chessrepertoiretrainer.core.chess.domain.findLegalMoveBySan
import com.example.chessrepertoiretrainer.core.chess.pgn.navigator.TreeGameNavigator
import com.example.chessrepertoiretrainer.core.engine.EngineAnalysisHolder
import com.example.chessrepertoiretrainer.core.engine.StockfishEngine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class AnalysisViewModel(engine: StockfishEngine, startFen: String? = null) : ViewModel() {

    val chessController: ChessBoardController = DefaultChessBoardController().also { ctrl ->
        if (startFen != null) ctrl.loadPositionFromFen(startFen)
    }

    val annotations = BoardAnnotations()
    val navigator = TreeGameNavigator(startFen ?: TreeGameNavigator.STARTING_FEN)

    private val engineHolder =
        EngineAnalysisHolder(engine, viewModelScope, chessController, autoEnable = true)
    val isEngineEnabled = engineHolder.isEnabled
    val engineAnalysis = engineHolder.analysis
    val engineSearchState = engineHolder.searchState
    val engineError = engineHolder.error

    init {
        chessController.onMoveApplied = { navigator.onUserMove(it) }
        navigator.onPositionChanged = { fen, lm -> chessController.loadPositionFromFen(fen, lm) }

        viewModelScope.launch {
            snapshotFlow { chessController.boardState }.distinctUntilChanged().collect {
                annotations.arrows = emptyList()
            }
        }
        viewModelScope.launch {
            engineAnalysis.collectLatest { analysis ->
                annotations.arrows = if (analysis != null && isEngineEnabled.value) {
                    val firstSan = analysis.line.trim().split(" ")
                        .firstOrNull { !it.contains(".") && it.isNotBlank() }
                    val move = firstSan?.let { chessController.getBoard().findLegalMoveBySan(it) }
                    if (move != null) listOf(Arrow(move.from, move.to)) else emptyList()
                } else emptyList()
            }
        }
    }

    fun toggleEngine() = engineHolder.toggle()
    fun analyzeDeeper() = engineHolder.analyzeDeeper()

    override fun onCleared() {
        super.onCleared()
        engineHolder.dispose()
    }

    class Factory(private val engine: StockfishEngine, private val startFen: String? = null) :
        ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
            AnalysisViewModel(engine, startFen) as T
    }
}
