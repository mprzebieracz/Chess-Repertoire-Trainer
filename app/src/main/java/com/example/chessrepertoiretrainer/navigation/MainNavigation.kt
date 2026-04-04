package com.example.chessrepertoiretrainer.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.chessrepertoiretrainer.database.ChessDatabase
import com.example.chessrepertoiretrainer.database.dao.RepertoireDao
import com.example.chessrepertoiretrainer.ui.screens.AnalysisScreen
import com.example.chessrepertoiretrainer.ui.screens.ChaptersScreen
import com.example.chessrepertoiretrainer.ui.screens.HomeScreen
import com.example.chessrepertoiretrainer.ui.screens.LineEditorScreen
import com.example.chessrepertoiretrainer.ui.screens.LinesScreen
import com.example.chessrepertoiretrainer.ui.screens.RepertoiresScreen
import com.example.chessrepertoiretrainer.ui.screens.SettingsScreen
import com.example.chessrepertoiretrainer.ui.screens.TrainScreen
import com.example.chessrepertoiretrainer.ui.screens.TrainSelectionScreen
import com.example.chessrepertoiretrainer.ui.screens.YourGamesScreen
import com.example.chessrepertoiretrainer.ui.viewmodels.AnalysisViewModel
import com.example.chessrepertoiretrainer.ui.viewmodels.ChaptersViewModel
import com.example.chessrepertoiretrainer.ui.viewmodels.LineEditorViewModel
import com.example.chessrepertoiretrainer.ui.viewmodels.LinesViewModel
import com.example.chessrepertoiretrainer.ui.viewmodels.RepertoiresViewModel
import com.example.chessrepertoiretrainer.ui.viewmodels.TrainingSelectionViewModel
import com.example.chessrepertoiretrainer.ui.viewmodels.TrainingViewModel


fun NavGraphBuilder.repertoireGraph(
    navController: NavHostController,
    repertoireDao: RepertoireDao
) {
    // 1. Główny Ekran Repertuarów
    composable(Screen.RepertoireMain.route) {
        val vm: RepertoiresViewModel =
            viewModel(factory = RepertoiresViewModel.Factory(repertoireDao))
        RepertoiresScreen(viewModel = vm, onNavigateToChapters = { id ->
            navController.navigate(Screen.Chapters.createRoute(id))
        })
    }

    // 2. Rozdziały
    composable(
        route = Screen.Chapters.route,
        arguments = listOf(navArgument("repertoireId") { type = NavType.IntType })
    ) {
        val vm: ChaptersViewModel = viewModel(factory = ChaptersViewModel.Factory(repertoireDao))
        ChaptersScreen(
            viewModel = vm,
            onNavigateToLines = { id -> navController.navigate(Screen.Lines.createRoute(id)) },
            onBackClick = { navController.popBackStack() }
        )
    }

    // 3. Linie
    composable(
        route = Screen.Lines.route,
        arguments = listOf(navArgument("chapterId") { type = NavType.IntType })
    ) {
        val vm: LinesViewModel = viewModel(factory = LinesViewModel.Factory(repertoireDao))
        LinesScreen(
            viewModel = vm,
            onNavigateToLineEditor = { id -> navController.navigate(Screen.LineEditor.createRoute(id)) },
            onBackClick = { navController.popBackStack() },
            onNavigateToTraining = { chapterId ->
                navController.navigate(Screen.ChapterTraining.createRoute(chapterId))
            }
        )
    }

    // 4. Edytor Linii
    composable(
        route = Screen.LineEditor.route,
        arguments = listOf(navArgument("lineId") { type = NavType.IntType })
    ) {
        val vm: LineEditorViewModel =
            viewModel(factory = LineEditorViewModel.Factory(repertoireDao))
        LineEditorScreen(
            viewModel = vm,
            onBackClick = { navController.popBackStack() }
        )
    }

    composable(
        route = Screen.ChapterTraining.route,
        arguments = listOf(navArgument("chapterId") { type = NavType.IntType })
    ) {
        val trainingViewModel: TrainingViewModel = viewModel(factory = TrainingViewModel.Factory(repertoireDao))
        TrainScreen(viewModel = trainingViewModel, onBackClick = { navController.popBackStack() })
    }
}


@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val repertoireDao = ChessDatabase.getDatabase(LocalContext.current).repertoireDao()

    Scaffold(
        bottomBar = { AppBottomBar(navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Ekrany główne
            composable(Screen.Home.route) { HomeScreen() }
            composable(Screen.Train.route) {
                val selectionViewModel: TrainingSelectionViewModel =
                    viewModel(factory = TrainingSelectionViewModel.Factory(repertoireDao))
                TrainSelectionScreen(
                    viewModel = selectionViewModel,
                    onStartTraining = { chapterId ->
                        navController.navigate(Screen.ChapterTraining.createRoute(chapterId))
                    }
                )
            }
            composable(Screen.YourGames.route) { YourGamesScreen() }
            composable(Screen.Settings.route) { SettingsScreen() }

            composable(Screen.Analysis.route) {
                val analysisViewModel: AnalysisViewModel = viewModel()
                AnalysisScreen(viewModel = analysisViewModel)
            }

            repertoireGraph(navController, repertoireDao)
        }
    }
}
