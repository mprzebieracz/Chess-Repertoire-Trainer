package com.example.chessrepertoiretrainer.navigation

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.example.chessrepertoiretrainer.data.PuzzleRepository
import com.example.chessrepertoiretrainer.ui.screens.PuzzleTrainingScreen
import com.example.chessrepertoiretrainer.ui.screens.PuzzlesScreen
import com.example.chessrepertoiretrainer.ui.viewmodels.PuzzleTrainingViewModel
import com.example.chessrepertoiretrainer.ui.viewmodels.PuzzlesViewModel

fun NavGraphBuilder.puzzlesGraph(
    navController: NavHostController, puzzleRepository: PuzzleRepository
) {
    composable(Screen.Puzzles.route) {
        val vm: PuzzlesViewModel = viewModel(factory = PuzzlesViewModel.Factory(puzzleRepository))
        PuzzlesScreen(
            viewModel = vm,
            onStartTraining = { navController.navigate(Screen.PuzzleTraining.route) })
    }

    composable(Screen.PuzzleTraining.route) {
        val vm: PuzzleTrainingViewModel =
            viewModel(factory = PuzzleTrainingViewModel.Factory(puzzleRepository))
        PuzzleTrainingScreen(viewModel = vm, onBackClick = { navController.popBackStack() })
    }
}
