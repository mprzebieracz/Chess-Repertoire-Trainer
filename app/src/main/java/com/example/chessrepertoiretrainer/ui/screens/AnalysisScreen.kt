package com.example.chessrepertoiretrainer.ui.screens

import androidx.compose.runtime.Composable
import com.example.chessrepertoiretrainer.ui.components.chess.ChessScreenLayout
import com.example.chessrepertoiretrainer.ui.viewmodels.AnalysisViewModel

@Composable
fun AnalysisScreen(viewModel: AnalysisViewModel) {
    ChessScreenLayout(
        title = "Analysis",
        chessCtrl = viewModel.chessController
    )
}