package com.example.chessrepertoiretrainer.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.NavDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.chessrepertoiretrainer.database.ChessDatabase
import com.example.chessrepertoiretrainer.data.ChessComGameFetcher
import com.example.chessrepertoiretrainer.data.GameFetcherRegistry
import com.example.chessrepertoiretrainer.data.PlayerGamesRepository
import com.example.chessrepertoiretrainer.data.PlayerProfileRepository
import com.example.chessrepertoiretrainer.domain.games.GamesRepository
import com.example.chessrepertoiretrainer.database.dao.RepertoireDao
import com.example.chessrepertoiretrainer.data.DefaultPuzzleRepository
import com.example.chessrepertoiretrainer.ui.screens.AnalysisScreen
import com.example.chessrepertoiretrainer.ui.screens.ChaptersScreen
import com.example.chessrepertoiretrainer.ui.screens.HomeScreen
import com.example.chessrepertoiretrainer.ui.screens.PuzzleTrainingScreen
import com.example.chessrepertoiretrainer.ui.screens.LineEditorScreen
import com.example.chessrepertoiretrainer.ui.screens.LinesScreen
import com.example.chessrepertoiretrainer.ui.screens.PuzzlesScreen
import com.example.chessrepertoiretrainer.ui.screens.RepertoiresScreen
import com.example.chessrepertoiretrainer.ui.screens.SettingsScreen
import com.example.chessrepertoiretrainer.ui.screens.TrainScreen
import com.example.chessrepertoiretrainer.ui.screens.TrainSelectionScreen
import com.example.chessrepertoiretrainer.ui.screens.PlayerProfilesScreen
import com.example.chessrepertoiretrainer.ui.screens.OpeningTreeScreen
import com.example.chessrepertoiretrainer.ui.viewmodels.AnalysisViewModel
import com.example.chessrepertoiretrainer.ui.viewmodels.ChaptersViewModel
import com.example.chessrepertoiretrainer.ui.viewmodels.LineEditorViewModel
import com.example.chessrepertoiretrainer.ui.viewmodels.LinesViewModel
import com.example.chessrepertoiretrainer.ui.viewmodels.PlayerProfilesViewModel
import com.example.chessrepertoiretrainer.ui.viewmodels.PuzzleTrainingViewModel
import com.example.chessrepertoiretrainer.ui.viewmodels.PuzzlesViewModel
import com.example.chessrepertoiretrainer.ui.viewmodels.RepertoiresViewModel
import com.example.chessrepertoiretrainer.ui.viewmodels.TrainingSelectionViewModel
import com.example.chessrepertoiretrainer.ui.viewmodels.TrainingViewModel
import com.example.chessrepertoiretrainer.ui.viewmodels.OpeningTreeViewModel


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
    val db = ChessDatabase.getDatabase(LocalContext.current)
    val repertoireDao = db.repertoireDao()
    val puzzleDao = db.puzzleDao()
    val playerProfileDao = db.playerProfileDao()
    val gameDao = db.gameDao()
    val puzzleRepository = DefaultPuzzleRepository(puzzleDao)
    val playerProfileRepository = PlayerProfileRepository(playerProfileDao)
    val gameFetcherRegistry = GameFetcherRegistry(
        listOf(
            com.example.chessrepertoiretrainer.data.LichessGameFetcher,
            ChessComGameFetcher
        )
    )
    val playerGamesRepository: GamesRepository = PlayerGamesRepository(
        gameDao = gameDao,
        playerProfileDao = playerProfileDao,
        fetcherRegistry = gameFetcherRegistry
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val showBottomBar = shouldShowBottomBar(currentDestination)

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                AppBottomBar(navController)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Ekrany główne
            composable(Screen.Home.route) {
                HomeScreen(
                    onOpenAnalysis = { navController.navigate(Screen.Analysis.route) }
                )
            }
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
             // "My games" root screen: configure filters and download games for analysis.
             composable(Screen.YourGames.route) {
                 val vm: PlayerProfilesViewModel =
                     viewModel(
                         factory = PlayerProfilesViewModel.Factory(
                             playerProfileRepository,
                             playerGamesRepository
                         )
                     )
                 PlayerProfilesScreen(
                     viewModel = vm,
                     onOpenProfileTree = { profileId, color, timeControl, maxGames ->
                         navController.navigate(
                             Screen.OpeningTree.createRoute(
                                 profileId = profileId,
                                 color = color,
                                 timeControl = timeControl,
                                 maxGames = maxGames
                             )
                         )
                     }
                 )
             }
            composable(Screen.Puzzles.route) {
                val puzzlesViewModel: PuzzlesViewModel =
                    viewModel(factory = PuzzlesViewModel.Factory(puzzleRepository))
                PuzzlesScreen(
                    viewModel = puzzlesViewModel,
                    onStartTraining = { navController.navigate(Screen.PuzzleTraining.route) }
                )
            }
            composable(
                route = Screen.OpeningTree.route,
                arguments = listOf(
                    navArgument("profileId") { type = NavType.LongType },
                    navArgument("color") { type = NavType.StringType; defaultValue = "both" },
                    navArgument("timeControl") { type = NavType.StringType; defaultValue = "" },
                    navArgument("maxGames") { type = NavType.IntType; defaultValue = -1 }
                )
            ) { backStackEntry ->
                val profileId = backStackEntry.arguments?.getLong("profileId") ?: return@composable
                val colorArg = backStackEntry.arguments?.getString("color") ?: "both"
                val timeControlArg = backStackEntry.arguments?.getString("timeControl") ?: ""
                val maxGamesArg = backStackEntry.arguments?.getInt("maxGames") ?: -1
                val maxGamesForTree = maxGamesArg.takeIf { it > 0 }

                val colorFilter = when (colorArg.lowercase()) {
                    "white" -> OpeningTreeViewModel.ColorFilter.WHITE_ONLY
                    "black" -> OpeningTreeViewModel.ColorFilter.BLACK_ONLY
                    else -> OpeningTreeViewModel.ColorFilter.BOTH
                }

                val vm: OpeningTreeViewModel =
                     viewModel(
                        factory = OpeningTreeViewModel.Factory(
                             profileId = profileId,
                             gamesRepository = playerGamesRepository,
                             colorFilter = colorFilter,
                             timeControlFilter = timeControlArg.ifBlank { null },
                             maxGamesForTree = maxGamesForTree
                         )
                     )
                OpeningTreeScreen(
                    viewModel = vm,
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable(Screen.Settings.route) { SettingsScreen() }

            composable(Screen.Analysis.route) {
                val analysisViewModel: AnalysisViewModel = viewModel()
                AnalysisScreen(
                    viewModel = analysisViewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(Screen.PuzzleTraining.route) {
                val trainingViewModel: PuzzleTrainingViewModel =
                    viewModel(factory = PuzzleTrainingViewModel.Factory(puzzleRepository))
                PuzzleTrainingScreen(
                    viewModel = trainingViewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }

            repertoireGraph(navController, repertoireDao)
        }
    }
}

private fun shouldShowBottomBar(destination: NavDestination?): Boolean {
    val route = destination?.route ?: return true

    return when {
        // Ekrany z szachownicą – pełny ekran, bez dolnego paska nawigacji
        route.startsWith(Screen.LineEditor.route.substringBefore("/")) -> false
        route.startsWith(Screen.ChapterTraining.route.substringBefore("/")) -> false
        route == Screen.Analysis.route -> false
        route == Screen.PuzzleTraining.route -> false
        route.startsWith(Screen.OpeningTree.route.substringBefore("/")) -> false
        else -> true
    }
}

