package com.example.chessrepertoiretrainer.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.chessrepertoiretrainer.data.GameForOpeningTree
import com.example.chessrepertoiretrainer.data.OpeningTree
import com.example.chessrepertoiretrainer.data.OpeningTreeBuilder
import com.example.chessrepertoiretrainer.data.PlayerGamesRepository
import com.example.chessrepertoiretrainer.ui.components.chess.DefaultChessBoardController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OpeningTreeViewModel(
  private val profileId: Long,
  private val gamesRepository: PlayerGamesRepository,
  private val colorFilter: ColorFilter = ColorFilter.BOTH,
  private val timeControlFilter: String? = null
) : ViewModel() {

    val chessController = DefaultChessBoardController()

    private var openingTree: OpeningTree? = null

    private val _uiState = MutableStateFlow(OpeningTreeUiState())
    val uiState: StateFlow<OpeningTreeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            rebuildOpeningTree()
        }
    }

      private suspend fun rebuildOpeningTree() {
        val allGamesWithPgn = gamesRepository.getGamesWithPgnForProfile(profileId)
        val gamesWithPgn = applyFilters(allGamesWithPgn)
        if (gamesWithPgn.isEmpty()) {
          openingTree = null
          _uiState.value = OpeningTreeUiState()
          chessController.resetBoard()
          return
        }

        val gamesForTree = gamesWithPgn.map { gwp ->
            GameForOpeningTree(
                pgn = gwp.pgn,
                isUserWhite = gwp.game.isUserWhite,
                resultTag = gwp.game.result
            )
        }

        openingTree = OpeningTreeBuilder.buildTree(gamesForTree)
        val tree = openingTree
        if (tree == null) {
            _uiState.value = OpeningTreeUiState()
            chessController.resetBoard()
        } else {
            applyFen(tree.rootFen, tree)
        }
    }

    private fun applyFen(fen: String, tree: OpeningTree) {
        val node = tree.getNode(fen) ?: run {
            _uiState.value = OpeningTreeUiState()
            chessController.resetBoard()
            return
        }

        val movesUi = node.children.values
            .sortedByDescending { it.games }
            .map { agg ->
                val total = if (agg.games <= 0) 1 else agg.games
                val winPercent = (agg.wins * 100) / total
                val drawPercent = (agg.draws * 100) / total
                val lossPercent = (agg.losses * 100) / total

                OpeningTreeMoveUi(
                    moveSan = agg.moveSan,
                    toFen = agg.toFen,
                    games = agg.games,
                    winPercent = winPercent,
                    drawPercent = drawPercent,
                    lossPercent = lossPercent
                )
            }

        val path = buildPathForNode(node, tree)

        _uiState.value = OpeningTreeUiState(
            currentFen = fen,
            pathMoves = path,
            moves = movesUi
        )

        chessController.loadPositionFromFen(fen)
    }

    private fun buildPathForNode(
        node: com.example.chessrepertoiretrainer.data.OpeningTreeNode,
        tree: OpeningTree
    ): List<String> {
        val path = mutableListOf<String>()
        var current: com.example.chessrepertoiretrainer.data.OpeningTreeNode? = node
        while (current != null && current.moveSanFromParent != null) {
            path.add(current.moveSanFromParent)
            val parentFen = current.parentFen
            current = if (parentFen != null) tree.getNode(parentFen) else null
        }
        return path.asReversed()
    }

    fun onMoveSelected(targetFen: String) {
        val tree = openingTree ?: return
        val node = tree.getNode(targetFen) ?: return
        applyFen(node.fen, tree)
    }

    fun onGoBack() {
        val tree = openingTree ?: return
        val currentFen = _uiState.value.currentFen ?: return
        val node = tree.getNode(currentFen) ?: return
        val parentFen = node.parentFen ?: return
        val parentNode = tree.getNode(parentFen) ?: return
        applyFen(parentNode.fen, tree)
    }

    fun onGoRoot() {
        val tree = openingTree ?: return
        applyFen(tree.rootFen, tree)
    }

    data class OpeningTreeMoveUi(
        val moveSan: String,
        val toFen: String,
        val games: Int,
        val winPercent: Int,
        val drawPercent: Int,
        val lossPercent: Int
    )

    data class OpeningTreeUiState(
        val currentFen: String? = null,
        val pathMoves: List<String> = emptyList(),
        val moves: List<OpeningTreeMoveUi> = emptyList()
    )

      enum class ColorFilter { BOTH, WHITE_ONLY, BLACK_ONLY }

      private fun applyFilters(games: List<com.example.chessrepertoiretrainer.data.GameWithPgn>): List<com.example.chessrepertoiretrainer.data.GameWithPgn> {
        var sequence = games.asSequence()

        sequence = when (colorFilter) {
          ColorFilter.BOTH -> sequence
          ColorFilter.WHITE_ONLY -> sequence.filter { it.game.isUserWhite }
          ColorFilter.BLACK_ONLY -> sequence.filter { !it.game.isUserWhite }
        }

        val tc = timeControlFilter?.trim().orEmpty()
        if (tc.isNotEmpty()) {
          sequence = sequence.filter { gwp ->
            gwp.game.timeControl?.contains(tc, ignoreCase = true) == true
          }
        }

        return sequence.toList()
      }

      class Factory(
        private val profileId: Long,
        private val gamesRepository: PlayerGamesRepository,
        private val colorFilter: ColorFilter,
        private val timeControlFilter: String?
      ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
          return OpeningTreeViewModel(
            profileId = profileId,
            gamesRepository = gamesRepository,
            colorFilter = colorFilter,
            timeControlFilter = timeControlFilter
          ) as T
        }
      }
}

