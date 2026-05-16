package com.example.chessrepertoiretrainer.feature.analysis

import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.chessrepertoiretrainer.core.chess.controller.ChessBoardController
import com.example.chessrepertoiretrainer.core.chess.controller.DefaultChessBoardController
import com.example.chessrepertoiretrainer.core.chess.pgn.navigator.TreeGameNavigator
import com.example.chessrepertoiretrainer.core.engine.EngineAnalysis
import com.example.chessrepertoiretrainer.core.engine.EngineSearchState
import com.example.chessrepertoiretrainer.core.engine.StockfishEngine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AnalysisViewModel(private val engine: StockfishEngine, startFen: String? = null) :
    ViewModel() {

    val chessController: ChessBoardController = DefaultChessBoardController().also { ctrl ->
        if (startFen != null) ctrl.loadPositionFromFen(startFen)
    }

    val navigator = TreeGameNavigator(startFen ?: TreeGameNavigator.STARTING_FEN)

    val isEngineEnabled: StateFlow<Boolean> =
        engine.isEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)
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

        engine.enable(chessController.boardState)
        viewModelScope.launch {
            snapshotFlow { chessController.boardState }.distinctUntilChanged().collect { fen ->
                if (engine.isEnabled.value) engine.updatePosition(fen)
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

    class Factory(private val engine: StockfishEngine, private val startFen: String? = null) :
        ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
            AnalysisViewModel(engine, startFen) as T
    }
}