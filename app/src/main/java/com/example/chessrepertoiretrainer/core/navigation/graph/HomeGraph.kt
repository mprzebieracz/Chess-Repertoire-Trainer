package com.example.chessrepertoiretrainer.core.navigation.graph

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.example.chessrepertoiretrainer.core.navigation.Screen
import com.example.chessrepertoiretrainer.feature.analysis.AnalysisScreen
import com.example.chessrepertoiretrainer.feature.analysis.AnalysisViewModel
import com.example.chessrepertoiretrainer.feature.home.HomeScreen
import com.example.chessrepertoiretrainer.feature.home.HomeViewModel
import com.example.chessrepertoiretrainer.feature.puzzles.PuzzleRepository
import com.example.chessrepertoiretrainer.feature.puzzles.presentation.PuzzleTrainingScreen
import com.example.chessrepertoiretrainer.feature.puzzles.presentation.PuzzleTrainingViewModel
import com.example.chessrepertoiretrainer.feature.settings.presentation.SettingsScreen
import com.example.chessrepertoiretrainer.feature.settings.presentation.SettingsViewModel

fun NavGraphBuilder.homeGraph(
    navController: NavHostController,
    settingsViewModel: SettingsViewModel,
    puzzleRepository: PuzzleRepository
) {
    composable(Screen.Home.route) {
        val vm: HomeViewModel = viewModel(factory = HomeViewModel.Factory(puzzleRepository))
        HomeScreen(
            viewModel = vm,
            onOpenAnalysis = { navController.navigate(Screen.Analysis.route) },
            onPlayDailyPuzzle = { navController.navigate(Screen.PuzzleTraining.route) }
        )
    }

    composable(Screen.Settings.route) {
        SettingsScreen(viewModel = settingsViewModel)
    }

    composable(Screen.Analysis.route) {
        val vm: AnalysisViewModel = viewModel()
        AnalysisScreen(viewModel = vm, onBackClick = { navController.popBackStack() })
    }

    composable(Screen.PuzzleTraining.route) {
        val vm: PuzzleTrainingViewModel = viewModel(
            factory = PuzzleTrainingViewModel.Factory(puzzleRepository)
        )
        PuzzleTrainingScreen(viewModel = vm, onBackClick = { navController.popBackStack() })
    }
}
