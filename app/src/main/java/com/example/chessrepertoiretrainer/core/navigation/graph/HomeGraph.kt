package com.example.chessrepertoiretrainer.core.navigation.graph

import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.example.chessrepertoiretrainer.app.AppContainer
import com.example.chessrepertoiretrainer.core.navigation.Screen
import com.example.chessrepertoiretrainer.feature.analysis.AnalysisScreen
import com.example.chessrepertoiretrainer.feature.analysis.AnalysisViewModel
import com.example.chessrepertoiretrainer.feature.home.HomeScreen
import com.example.chessrepertoiretrainer.feature.home.HomeViewModel
import com.example.chessrepertoiretrainer.feature.puzzles.PuzzleRepository
import com.example.chessrepertoiretrainer.feature.puzzles.presentation.OpeningPuzzleSessionScreen
import com.example.chessrepertoiretrainer.feature.puzzles.presentation.OpeningPuzzleSessionViewModel
import com.example.chessrepertoiretrainer.feature.puzzles.presentation.PuzzleTrainingScreen
import com.example.chessrepertoiretrainer.feature.puzzles.presentation.PuzzleTrainingViewModel
import com.example.chessrepertoiretrainer.feature.puzzles.presentation.RepertoirePuzzlesScreen
import com.example.chessrepertoiretrainer.feature.puzzles.presentation.RepertoirePuzzlesViewModel
import com.example.chessrepertoiretrainer.feature.repertoire.domain.RepertoireRepository
import com.example.chessrepertoiretrainer.feature.settings.presentation.SettingsScreen
import com.example.chessrepertoiretrainer.feature.settings.presentation.SettingsViewModel

fun NavGraphBuilder.homeGraph(
    navController: NavHostController,
    settingsViewModel: SettingsViewModel,
    puzzleRepository: PuzzleRepository,
    repertoireRepository: RepertoireRepository,
    appContainer: AppContainer
) {
    composable(Screen.Home.route) {
        val vm: HomeViewModel = viewModel(
            factory = HomeViewModel.Factory(
                puzzleRepository,
                appContainer.activityRecorder
            )
        )
        HomeScreen(
            viewModel = vm,
            onOpenAnalysis = { navController.navigate(Screen.Analysis.route) },
            onPlayDailyPuzzle = { navController.navigate(Screen.PuzzleTraining.route) },
            onOpenRepertoire = {
                navController.navigate(Screen.RepertoireMain.route) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            onOpenRepertoirePuzzles = { navController.navigate(Screen.RepertoirePuzzles.route) }
        )
    }

    composable(Screen.Settings.route) {
        SettingsScreen(viewModel = settingsViewModel)
    }

    composable(Screen.Analysis.route) {
        val startFen = remember { appContainer.navTransientStore.takeAnalysisStartFen() }
        val vm: AnalysisViewModel =
            viewModel(factory = AnalysisViewModel.Factory(appContainer.stockfishEngine, startFen))
        AnalysisScreen(viewModel = vm, onBackClick = { navController.popBackStack() })
    }

    composable(Screen.PuzzleTraining.route) {
        val puzzleId = remember { appContainer.navTransientStore.takePendingPuzzleId() }
        val vm: PuzzleTrainingViewModel =
            viewModel(factory = PuzzleTrainingViewModel.Factory(puzzleRepository, puzzleId))
        PuzzleTrainingScreen(viewModel = vm, onBackClick = { navController.popBackStack() })
    }

    composable(Screen.RepertoirePuzzles.route) {
        val vm: RepertoirePuzzlesViewModel = viewModel(
            factory = RepertoirePuzzlesViewModel.Factory(
                puzzleRepository,
                repertoireRepository,
                appContainer.openingRegistry,
            )
        )
        RepertoirePuzzlesScreen(
            viewModel = vm,
            onBackClick = { navController.popBackStack() },
            onStartSession = { openings ->
                appContainer.navTransientStore.selectedOpenings = openings
                navController.navigate(Screen.OpeningPuzzleSession.route)
            },
        )
    }

    composable(Screen.OpeningPuzzleSession.route) {
        val openings = remember { appContainer.navTransientStore.takeSelectedOpenings() }
        val vm: OpeningPuzzleSessionViewModel = viewModel(
            factory = OpeningPuzzleSessionViewModel.Factory(
                puzzleRepository,
                openings ?: emptyList(),
            )
        )
        OpeningPuzzleSessionScreen(vm, onBackClick = { navController.popBackStack() })
    }
}
