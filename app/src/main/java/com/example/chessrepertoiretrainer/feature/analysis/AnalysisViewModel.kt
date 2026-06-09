package com.example.chessrepertoiretrainer.feature.analysis

import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.chessrepertoiretrainer.core.bluetooth.BluetoothAnalysisSession
import com.example.chessrepertoiretrainer.core.bluetooth.BluetoothTransfer
import com.example.chessrepertoiretrainer.core.bluetooth.PairedDeviceUi
import com.example.chessrepertoiretrainer.core.chess.controller.BoardAnnotations
import com.example.chessrepertoiretrainer.core.chess.controller.ChessBoardController
import com.example.chessrepertoiretrainer.core.chess.controller.DefaultChessBoardController
import com.example.chessrepertoiretrainer.core.chess.domain.Arrow
import com.example.chessrepertoiretrainer.core.chess.domain.findLegalMoveBySan
import com.example.chessrepertoiretrainer.core.chess.domain.uciToMove
import com.example.chessrepertoiretrainer.core.chess.pgn.navigator.TreeGameNavigator
import com.example.chessrepertoiretrainer.core.engine.EngineAnalysisHolder
import com.example.chessrepertoiretrainer.core.engine.StockfishEngine
import com.github.bhlangonijr.chesslib.Piece
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class AnalysisViewModel(
    engine: StockfishEngine,
    private val bluetoothTransfer: BluetoothTransfer,
    startFen: String? = null,
) : ViewModel() {

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

    private val bluetoothSession = bluetoothTransfer.newAnalysisSession()
    val bluetoothState: StateFlow<BluetoothAnalysisSession.State> = bluetoothSession.state

    private val _pairedDevices = MutableStateFlow<List<PairedDeviceUi>>(emptyList())
    val pairedDevices: StateFlow<List<PairedDeviceUi>> = _pairedDevices.asStateFlow()

    private var sessionJob: Job? = null
    private var applyingRemoteMove = false

    init {
        chessController.onMoveApplied = { applied ->
            navigator.onUserMove(applied)
            if (!applyingRemoteMove) {
                sendMoveOverBluetooth(applied.move)
            }
        }

        viewModelScope.launch {
            bluetoothSession.incomingMoves.collect { uci -> applyMoveFromPeer(uci) }
        }

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

    private fun applyMoveFromPeer(uci: String) {
        val candidate = uciToMove(uci, chessController.getBoard()) ?: return
        val legalMove = chessController.getBoard().legalMoves().find { lm ->
            lm.from == candidate.from && lm.to == candidate.to &&
                    (candidate.promotion == Piece.NONE || lm.promotion == candidate.promotion)
        } ?: return
        applyingRemoteMove = true
        try {
            chessController.tryApplyMove(legalMove)
        } finally {
            applyingRemoteMove = false
        }
    }

    private fun sendMoveOverBluetooth(move: com.github.bhlangonijr.chesslib.move.Move) {
        if (bluetoothSession.state.value !is BluetoothAnalysisSession.State.Connected) return
        val promotionSuffix = when (move.promotion) {
            Piece.WHITE_QUEEN, Piece.BLACK_QUEEN -> "q"
            Piece.WHITE_ROOK, Piece.BLACK_ROOK -> "r"
            Piece.WHITE_BISHOP, Piece.BLACK_BISHOP -> "b"
            Piece.WHITE_KNIGHT, Piece.BLACK_KNIGHT -> "n"
            else -> ""
        }
        bluetoothSession.sendMove(move.from.name.lowercase() + move.to.name.lowercase() + promotionSuffix)
    }

    fun refreshPairedDevices() {
        _pairedDevices.value = bluetoothTransfer.pairedDevices().map {
            PairedDeviceUi(
                name = runCatching { it.name }.getOrNull() ?: "Unknown",
                address = it.address,
                device = it,
            )
        }
    }

    fun hostBluetoothSession() {
        sessionJob?.cancel()
        sessionJob = viewModelScope.launch { bluetoothSession.host() }
    }

    fun connectBluetooth(device: PairedDeviceUi) {
        sessionJob?.cancel()
        sessionJob = viewModelScope.launch { bluetoothSession.connect(device.device) }
    }

    fun disconnectBluetooth() {
        bluetoothSession.disconnect()
        sessionJob?.cancel()
    }

    fun toggleEngine() = engineHolder.toggle()
    fun analyzeDeeper() = engineHolder.analyzeDeeper()

    override fun onCleared() {
        super.onCleared()
        engineHolder.dispose()
        bluetoothSession.disconnect()
    }

    class Factory(
        private val engine: StockfishEngine,
        private val bluetoothTransfer: BluetoothTransfer,
        private val startFen: String? = null,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
            AnalysisViewModel(engine, bluetoothTransfer, startFen) as T
    }
}
