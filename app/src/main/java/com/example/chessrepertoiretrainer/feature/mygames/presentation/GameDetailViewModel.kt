package com.example.chessrepertoiretrainer.feature.mygames.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.chessrepertoiretrainer.core.chess.controller.DefaultChessBoardController
import com.example.chessrepertoiretrainer.core.chess.domain.moveFromSan
import com.example.chessrepertoiretrainer.core.database.entity.SavedGame
import com.example.chessrepertoiretrainer.core.chess.utils.PGNExtractor

class GameDetailViewModel(val game: SavedGame) : ViewModel() {

    val chessController = DefaultChessBoardController()

    init {
        loadGame()
    }

    private fun loadGame() {
        chessController.resetBoard()
        val sanMoves = PGNExtractor.extractSanMovesFromPgn(game.pgn)
        val board = chessController.getBoard()
        for (san in sanMoves) {
            val move = board.moveFromSan(san) ?: break
            chessController.onMove(move)
        }
        // Navigate back to the start so the user can replay from move 1
        repeat(sanMoves.size) { chessController.navigateBack() }

        if (game.isPlayerWhite == chessController.isFlipped) {
            chessController.flipBoard()
        }
    }

    class Factory(private val game: SavedGame) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            return GameDetailViewModel(game) as T
        }
    }
}
