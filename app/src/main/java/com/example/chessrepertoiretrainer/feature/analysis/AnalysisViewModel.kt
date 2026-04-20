package com.example.chessrepertoiretrainer.feature.analysis

import androidx.lifecycle.ViewModel
import com.example.chessrepertoiretrainer.core.chess.controller.ChessBoardController
import com.example.chessrepertoiretrainer.core.chess.controller.DefaultChessBoardController

class AnalysisViewModel : ViewModel() {
    val chessController: ChessBoardController = DefaultChessBoardController()
}