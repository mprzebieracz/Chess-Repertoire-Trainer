package com.example.chessrepertoiretrainer.feature.openingtree.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.chessrepertoiretrainer.core.chess.controller.BoardAnnotations
import com.example.chessrepertoiretrainer.core.chess.controller.DefaultChessBoardController
import com.example.chessrepertoiretrainer.core.chess.domain.Arrow
import com.example.chessrepertoiretrainer.core.chess.domain.findLegalMoveBySan
import com.example.chessrepertoiretrainer.core.network.explorer.LichessExplorerService
import com.example.chessrepertoiretrainer.core.network.explorer.MasterGameEntry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ExplorerSource { LICHESS_PLAYERS, MASTERS }

data class ExplorerUiState(
    val source: ExplorerSource = ExplorerSource.LICHESS_PLAYERS,
    val isFetching: Boolean = false,
    val moves: List<OpeningTreeMoveUi> = emptyList(),
    val topGames: List<MasterGameEntry> = emptyList(),
    val canGoBack: Boolean = false,
    val statusMessage: String? = null
)

class OpeningExplorerViewModel(
    private val explorerService: LichessExplorerService,
    startFen: String?
) : ViewModel() {

    val chessController = DefaultChessBoardController()
    val annotations = BoardAnnotations()

    private val _uiState = MutableStateFlow(ExplorerUiState())
    val uiState: StateFlow<ExplorerUiState> = _uiState.asStateFlow()

    private val fenHistory = mutableListOf<String>()
    private var fetchJob: Job? = null

    private val rootFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

    init {
        val initial = startFen ?: rootFen
        chessController.onMoveApplied = { applied ->
            if (fenHistory.isEmpty() || fenHistory.last() != applied.fenAfter) {
                fetchForFen(applied.fenAfter)
            }
        }
        if (startFen != null) chessController.loadPositionFromFen(startFen)
        fetchForFen(initial)
    }

    fun setSource(source: ExplorerSource) {
        _uiState.update { it.copy(source = source, moves = emptyList(), topGames = emptyList()) }
        val fen = fenHistory.lastOrNull() ?: rootFen
        fetchForFen(fen)
    }

    fun onMoveSelected(moveSan: String) {
        val move = chessController.getBoard().findLegalMoveBySan(moveSan) ?: return
        chessController.onMove(move)
    }

    fun onGoBack() {
        if (fenHistory.size < 2) return
        fenHistory.removeAt(fenHistory.lastIndex)
        val parentFen = fenHistory.last()
        chessController.loadPositionFromFen(parentFen)
        fetchForFen(parentFen, pushToHistory = false)
    }

    fun onGoRoot() {
        fenHistory.clear()
        chessController.loadPositionFromFen(rootFen)
        fetchForFen(rootFen)
    }

    fun openMasterGame(gameId: String, onReady: (String) -> Unit) {
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            _uiState.update { it.copy(isFetching = true) }
            val pgn = explorerService.getMasterGamePgn(gameId)
            _uiState.update { it.copy(isFetching = false) }
            if (pgn != null) onReady(pgn)
        }
    }

    private fun fetchForFen(fen: String, pushToHistory: Boolean = true) {
        fetchJob?.cancel()
        if (pushToHistory && (fenHistory.isEmpty() || fenHistory.last() != fen)) {
            fenHistory.add(fen)
        }
        _uiState.update {
            it.copy(
                isFetching = true, moves = emptyList(), topGames = emptyList(),
                canGoBack = fenHistory.size > 1
            )
        }

        fetchJob = viewModelScope.launch {
            try {
                val source = _uiState.value.source
                val response = when (source) {
                    ExplorerSource.LICHESS_PLAYERS -> explorerService.getPlayerStats(fen)
                    ExplorerSource.MASTERS -> explorerService.getMastersStats(fen)
                }

                if (response == null) {
                    _uiState.update {
                        it.copy(
                            isFetching = false,
                            statusMessage = "No data for this position.",
                            canGoBack = fenHistory.size > 1
                        )
                    }
                    return@launch
                }
                if (response.requiresAuth) {
                    val label =
                        if (source == ExplorerSource.MASTERS) "Masters database" else "Lichess player explorer"
                    _uiState.update {
                        it.copy(
                            isFetching = false,
                            statusMessage = "$label requires a Lichess API token. This will be added in a future update.",
                            canGoBack = fenHistory.size > 1
                        )
                    }
                    return@launch
                }

                val movesUi = response.moves.map { m ->
                    val t = m.total.coerceAtLeast(1L)
                    OpeningTreeMoveUi(
                        moveSan = m.san,
                        toFen = "",
                        games = m.total,
                        winPercent = ((m.white * 100L) / t).toInt(),
                        drawPercent = ((m.draws * 100L) / t).toInt(),
                        lossPercent = ((m.black * 100L) / t).toInt()
                    )
                }

                _uiState.update {
                    it.copy(
                        isFetching = false,
                        statusMessage = null,
                        moves = movesUi,
                        topGames = if (source == ExplorerSource.MASTERS) response.topGames else emptyList(),
                        canGoBack = fenHistory.size > 1
                    )
                }

                val totalGames = movesUi.sumOf { it.games }.coerceAtLeast(1L)
                val board = chessController.getBoard()
                annotations.arrows = movesUi.mapNotNull { moveUi ->
                    val move = board.findLegalMoveBySan(moveUi.moveSan) ?: return@mapNotNull null
                    val pct = moveUi.games.toFloat() / totalGames.toFloat()
                    val alpha = when {
                        pct >= 0.50f -> 1.00f
                        pct >= 0.25f -> 0.80f
                        pct >= 0.10f -> 0.60f
                        pct >= 0.03f -> 0.42f
                        else -> 0.28f
                    }
                    Arrow(move.from, move.to, alpha)
                }
            }
            catch (e: CancellationException) {
                throw e
            }
            catch (e: Exception) {
                android.util.Log.e("OpeningExplorerVM", "fetchForFen failed: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        isFetching = false,
                        statusMessage = "Error: ${e.message}",
                        canGoBack = fenHistory.size > 1
                    )
                }
            }
        }
    }

    class Factory(
        private val explorerService: LichessExplorerService,
        private val startFen: String? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            OpeningExplorerViewModel(explorerService, startFen) as T
    }
}