package com.example.chessrepertoiretrainer.core.navigation.graph

import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.chessrepertoiretrainer.app.AppContainer
import com.example.chessrepertoiretrainer.core.navigation.Screen
import com.example.chessrepertoiretrainer.feature.mygames.presentation.AccountStatsScreen
import com.example.chessrepertoiretrainer.feature.mygames.presentation.AccountStatsViewModel
import com.example.chessrepertoiretrainer.feature.mygames.presentation.GameDetailScreen
import com.example.chessrepertoiretrainer.feature.mygames.presentation.GameDetailViewModel
import com.example.chessrepertoiretrainer.feature.mygames.presentation.GamesListScreen
import com.example.chessrepertoiretrainer.feature.mygames.presentation.GamesListViewModel
import com.example.chessrepertoiretrainer.feature.mygames.presentation.MyGamesScreen
import com.example.chessrepertoiretrainer.feature.mygames.presentation.MyGamesViewModel

fun NavGraphBuilder.myGamesGraph(navController: NavHostController, appContainer: AppContainer) {
    composable(Screen.MyGames.route) {
        val vm: MyGamesViewModel =
            viewModel(
                factory = MyGamesViewModel.Factory(
                    syncManager = appContainer.gameSyncManager,
                    settingsRepository = appContainer.userSettingsRepository,
                    savedGameRepository = appContainer.savedGameRepository
                )
            )
        MyGamesScreen(viewModel = vm, onOpenLichess = { username ->
            navController.navigate(Screen.AccountStats.createRoute("lichess", username))
        }, onOpenChessCom = { username ->
            navController.navigate(Screen.AccountStats.createRoute("chess.com", username))
        }, onOpenGamesList = { navController.navigate(Screen.GamesList.route) })
    }

    composable(
        route = Screen.AccountStats.route,
        arguments = listOf(
            navArgument("platform") { type = NavType.StringType },
            navArgument("username") {
                type = NavType.StringType
            })
    ) { backStackEntry ->
        val platform = backStackEntry.arguments?.getString("platform") ?: return@composable
        val username = backStackEntry.arguments?.getString("username") ?: return@composable
        val vm: AccountStatsViewModel =
            viewModel(
                factory = AccountStatsViewModel.Factory(
                    platform = platform,
                    username = username,
                    repository = appContainer.savedGameRepository
                )
            )
        AccountStatsScreen(viewModel = vm, onBackClick = { navController.popBackStack() })
    }

    composable(Screen.GamesList.route) {
        val vm: GamesListViewModel =
            viewModel(factory = GamesListViewModel.Factory(appContainer.savedGameRepository))
        GamesListScreen(viewModel = vm, onOpenGame = { game ->
            appContainer.navTransientStore.savedGame = game
            navController.navigate(Screen.GameDetail.createRoute(game.id))
        }, onBackClick = { navController.popBackStack() })
    }

    composable(
        route = Screen.GameDetail.route,
        arguments = listOf(navArgument("gameId") { type = NavType.StringType })
    ) {
        val game = remember { appContainer.navTransientStore.takeSavedGame() }
        if (game != null) {
            val vm: GameDetailViewModel = viewModel(
                factory = GameDetailViewModel.Factory(
                    game,
                    appContainer.repertoireComplianceAnalyzer,
                    appContainer.stockfishEngine
                )
            )
            GameDetailScreen(
                viewModel = vm,
                onBackClick = { navController.popBackStack() },
                onViewInCourse = { chapterId, lineId ->
                    navController.navigate(
                        Screen.ChapterReview.createRoute(
                            chapterId,
                            lineId
                        )
                    )
                })
        }
    }
}