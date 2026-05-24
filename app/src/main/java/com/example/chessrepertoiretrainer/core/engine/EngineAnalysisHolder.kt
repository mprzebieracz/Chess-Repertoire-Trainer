package com.example.chessrepertoiretrainer.core.engine

import androidx.compose.runtime.snapshotFlow
import com.example.chessrepertoiretrainer.core.chess.controller.ChessBoardController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Wires [StockfishEngine] to a [ChessBoardController]: forwards position
 * changes, exposes engine StateFlows, and centralizes toggle/deeper/dispose.
 * Use one per ViewModel that needs engine analysis.
 */
class EngineAnalysisHolder(
    private val engine: StockfishEngine,
    scope: CoroutineScope,
    private val controller: ChessBoardController,
    autoEnable: Boolean = false,
) {
    val isEnabled: StateFlow<Boolean> =
        engine.isEnabled.stateIn(scope, SharingStarted.WhileSubscribed(5_000), autoEnable)
    val analysis: StateFlow<EngineAnalysis?> =
        engine.analysis.stateIn(scope, SharingStarted.WhileSubscribed(5_000), null)
    val searchState: StateFlow<EngineSearchState> =
        engine.searchState.stateIn(
            scope,
            SharingStarted.WhileSubscribed(5_000),
            EngineSearchState.IDLE,
        )
    val error: StateFlow<String?> =
        engine.engineError.stateIn(scope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        if (autoEnable) engine.enable(controller.boardState)
        scope.launch {
            snapshotFlow { controller.boardState }.distinctUntilChanged().collect { fen ->
                if (engine.isEnabled.value) engine.updatePosition(fen)
            }
        }
    }

    fun toggle() {
        if (engine.isEnabled.value) engine.disable()
        else engine.enable(controller.boardState)
    }

    fun analyzeDeeper() = engine.analyzeDeeper()

    /** Call from `ViewModel.onCleared()`. */
    fun dispose() = engine.disable()
}
