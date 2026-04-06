package com.example.chessrepertoiretrainer.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.example.chessrepertoiretrainer.data.DefaultGameStatsRepository
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
import com.example.chessrepertoiretrainer.ui.screens.CourseOverviewScreen
import com.example.chessrepertoiretrainer.ui.screens.TrainScreen
import com.example.chessrepertoiretrainer.ui.screens.LearnChapterScreen
import com.example.chessrepertoiretrainer.ui.screens.TrainSelectionScreen
import com.example.chessrepertoiretrainer.ui.screens.OpeningTreeSearchScreen
import com.example.chessrepertoiretrainer.ui.screens.OpeningTreeScreen
import com.example.chessrepertoiretrainer.ui.screens.MyStatsScreen
import com.example.chessrepertoiretrainer.ui.screens.ReviewChapterScreen
import com.example.chessrepertoiretrainer.ui.viewmodels.AnalysisViewModel
import com.example.chessrepertoiretrainer.ui.viewmodels.ChaptersViewModel
import com.example.chessrepertoiretrainer.ui.viewmodels.LineEditorViewModel
import com.example.chessrepertoiretrainer.ui.viewmodels.LinesViewModel
import com.example.chessrepertoiretrainer.ui.viewmodels.PuzzleTrainingViewModel
import com.example.chessrepertoiretrainer.ui.viewmodels.PuzzlesViewModel
import com.example.chessrepertoiretrainer.ui.viewmodels.RepertoiresViewModel
import com.example.chessrepertoiretrainer.ui.viewmodels.CourseOverviewViewModel
import com.example.chessrepertoiretrainer.ui.viewmodels.TrainingSelectionViewModel
import com.example.chessrepertoiretrainer.ui.viewmodels.TrainingViewModel
import com.example.chessrepertoiretrainer.ui.viewmodels.LearnChapterViewModel
import com.example.chessrepertoiretrainer.ui.viewmodels.ReviewChapterViewModel
import com.example.chessrepertoiretrainer.ui.viewmodels.OpeningTreeViewModel
import com.example.chessrepertoiretrainer.ui.viewmodels.SettingsViewModel
import com.example.chessrepertoiretrainer.ui.viewmodels.OpeningTreeSearchViewModel
import com.example.chessrepertoiretrainer.ui.viewmodels.MyStatsViewModel


fun NavGraphBuilder.repertoireGraph(
    navController: NavHostController,
    repertoireDao: RepertoireDao
) {
    // 1. Główny Ekran Repertuarów (lista kursów)
    composable(Screen.RepertoireMain.route) {
        val vm: RepertoiresViewModel =
            viewModel(factory = RepertoiresViewModel.Factory(repertoireDao))
        RepertoiresScreen(viewModel = vm, onNavigateToChapters = { id ->
            // When user taps a repertoire, open the course overview (learn
            // mode) by default. Editing is available from that screen.
            navController.navigate(Screen.CourseOverview.createRoute(id))
        })
    }

    // 1b. Course overview (learn/train/edit entry point for a repertoire)
    composable(
        route = Screen.CourseOverview.route,
        arguments = listOf(navArgument("repertoireId") { type = NavType.IntType })
    ) {
        val vm: CourseOverviewViewModel =
            viewModel(factory = CourseOverviewViewModel.Factory(repertoireDao))
        CourseOverviewScreen(
            viewModel = vm,
            onBackClick = { navController.popBackStack() },
            onEditCourse = { repertoireId ->
                navController.navigate(Screen.Chapters.createRoute(repertoireId))
            },
            onTrainCourse = { _ ->
                // For now, training the course is equivalent to training all
                // chapters selected via the existing training tab. In a later
                // step we can introduce a dedicated "train course" flow.
                navController.navigate(Screen.Train.route)
            },
            onOpenChapterLearn = { chapterId ->
                // Open the dedicated learn flow for this chapter.
                navController.navigate(Screen.ChapterLearn.createRoute(chapterId))
            },
            onOpenChapterTrain = { chapterId ->
                navController.navigate(Screen.ChapterTraining.createRoute(chapterId))
            },
            onOpenChapterReview = { chapterId ->
                navController.navigate(Screen.ChapterReview.createRoute(chapterId))
            }
        )
    }

    // 1c. Learn chapter flow (step through each line, then train it)
    composable(
        route = Screen.ChapterLearn.route,
        arguments = listOf(navArgument("chapterId") { type = NavType.IntType })
    ) { backStackEntry ->
        val vm: LearnChapterViewModel = viewModel(factory = LearnChapterViewModel.Factory(repertoireDao))

        // Listen for a signal that single-line training has finished and
        // advance the learn flow when it happens.
        val savedStateHandle = backStackEntry.savedStateHandle
        val lineTrainingFinishedFlow = savedStateHandle.getStateFlow("lineTrainingFinished", false)
        val lineTrainingFinished by lineTrainingFinishedFlow.collectAsStateWithLifecycle()

        LaunchedEffect(lineTrainingFinished) {
            if (lineTrainingFinished) {
                vm.onLineTrainingFinished()
                savedStateHandle["lineTrainingFinished"] = false
            }
        }

        LearnChapterScreen(
            viewModel = vm,
            onBackClick = { navController.popBackStack() },
            onStartChapterTraining = { chapterId ->
                navController.navigate(Screen.ChapterTraining.createRoute(chapterId))
            },
            onStartLineTraining = { lineId ->
                navController.navigate(Screen.LineTraining.createRoute(lineId))
            }
        )
    }

    // 1d. Review chapter flow (read-only browsing of lines)
    composable(
        route = Screen.ChapterReview.route,
        arguments = listOf(navArgument("chapterId") { type = NavType.IntType })
    ) {
        val vm: ReviewChapterViewModel = viewModel(factory = ReviewChapterViewModel.Factory(repertoireDao))
        ReviewChapterScreen(
            viewModel = vm,
            onBackClick = { navController.popBackStack() }
        )
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

    // 5. Single-line training, used from the learn flow
    composable(
        route = Screen.LineTraining.route,
        arguments = listOf(navArgument("lineId") { type = NavType.IntType })
    ) { backStackEntry ->
        val trainingViewModel: TrainingViewModel = viewModel(factory = TrainingViewModel.Factory(repertoireDao))
        TrainScreen(
            viewModel = trainingViewModel,
            onBackClick = { navController.popBackStack() },
            onSessionComplete = {
                // Notify the learn screen that this line's training has finished
                navController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.set("lineTrainingFinished", true)
                navController.popBackStack()
            }
        )
    }
}


@Composable
fun AppNavigation(settingsViewModel: SettingsViewModel) {
    val navController = rememberNavController()
    val db = ChessDatabase.getDatabase(LocalContext.current)
    val repertoireDao = db.repertoireDao()
    val puzzleDao = db.puzzleDao()
    val playerProfileDao = db.playerProfileDao()
    val gameDao = db.gameDao()
    val gameStatsDao = db.gameStatsDao()
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
    val gameStatsRepository = DefaultGameStatsRepository(
        gamesRepository = playerGamesRepository,
        gameStatsDao = gameStatsDao
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
                     onOpenAnalysis = { navController.navigate(Screen.Analysis.route) },
                     onOpenMyStats = { navController.navigate(Screen.MyStats.route) }
                 )
             }
             composable(Screen.Train.route) {
                val selectionViewModel: TrainingSelectionViewModel =
                    viewModel(factory = TrainingSelectionViewModel.Factory(repertoireDao))
                TrainSelectionScreen(
                    viewModel = selectionViewModel,
                    onStartTraining = { chapterId ->
                        navController.navigate(Screen.ChapterTraining.createRoute(chapterId))
                    },
                    onBackClick = { navController.popBackStack() }
                )
            }
             // Opening tree search for arbitrary players (ephemeral, in-memory).
             composable(Screen.YourGames.route) {
                 val vm: OpeningTreeSearchViewModel =
                     viewModel(
                         factory = OpeningTreeSearchViewModel.Factory(
                             gameFetcherRegistry
                         )
                     )

                 val settings by settingsViewModel.settings.collectAsStateWithLifecycle()

                 OpeningTreeSearchScreen(
                     viewModel = vm,
                     defaultLichessUsername = settings.lichessUsername,
                     defaultChessComUsername = settings.chessComUsername,
                     defaultPlatform = settings.defaultOnlinePlatform,
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
             composable(Screen.Settings.route) { SettingsScreen(viewModel = settingsViewModel) }

             composable(Screen.MyStats.route) {
                 val vm: MyStatsViewModel = viewModel(
                     factory = MyStatsViewModel.Factory(
                         profileRepository = playerProfileRepository,
                          gamesRepository = playerGamesRepository,
                          gameStatsRepository = gameStatsRepository
                     )
                 )

                 val settings by settingsViewModel.settings.collectAsStateWithLifecycle()

                 MyStatsScreen(
                     viewModel = vm,
                     settings = settings,
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
        // Ekrany z szachownicą, które mają własny przycisk "Back" i
        // powinny zajmować cały ekran (bez dolnego paska nawigacji).
        route.startsWith(Screen.LineEditor.route.substringBefore("/")) -> false
        route.startsWith(Screen.ChapterLearn.route.substringBefore("/")) -> false
        route.startsWith(Screen.ChapterReview.route.substringBefore("/")) -> false
        route.startsWith(Screen.LineTraining.route.substringBefore("/")) -> false
        route == Screen.Analysis.route -> false
        route == Screen.PuzzleTraining.route -> false
        route.startsWith(Screen.OpeningTree.route.substringBefore("/")) -> false
        // Dla ekranów treningu całego rozdziału (ChapterTraining) zostawiamy
        // dolny pasek nawigacji widoczny, żeby zawsze można było szybko
        // wrócić np. do zakładki "Repertoire".
        else -> true
    }
}

