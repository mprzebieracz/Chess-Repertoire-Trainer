package com.example.chessrepertoiretrainer.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.example.chessrepertoiretrainer.ui.components.chess.ChessBoardController
import com.example.chessrepertoiretrainer.ui.components.chess.DefaultChessBoardController

class AnalysisViewModel : ViewModel() {
  val chessController: ChessBoardController = DefaultChessBoardController()
}