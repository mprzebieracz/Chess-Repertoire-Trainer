package com.example.chessrepertoiretrainer.core.navigation.graph

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.example.chessrepertoiretrainer.core.navigation.Screen
import com.example.chessrepertoiretrainer.feature.puzzles.PuzzleRepository
import com.example.chessrepertoiretrainer.feature.puzzles.presentation.PuzzleTrainingScreen
import com.example.chessrepertoiretrainer.feature.puzzles.presentation.PuzzleTrainingViewModel
import com.example.chessrepertoiretrainer.feature.puzzles.presentation.PuzzlesScreen
import com.example.chessrepertoiretrainer.feature.puzzles.presentation.PuzzlesViewModel

fun NavGraphBuilder.puzzlesGraph(
    navController: NavHostController, puzzleRepository: PuzzleRepository
) {
    composable(Screen.Puzzles.route) {
        val vm: PuzzlesViewModel = viewModel(factory = PuzzlesViewModel.Factory(puzzleRepository))
        PuzzlesScreen(
            viewModel = vm, onStartTraining = { navController.navigate(Screen.PuzzleTraining.route) })
    }

    composable(Screen.PuzzleTraining.route) {
        val vm: PuzzleTrainingViewModel = viewModel(
            factory = PuzzleTrainingViewModel.Factory(puzzleRepository)
        )
        PuzzleTrainingScreen(
            viewModel = vm, onBackClick = { navController.popBackStack() })
    }
}
