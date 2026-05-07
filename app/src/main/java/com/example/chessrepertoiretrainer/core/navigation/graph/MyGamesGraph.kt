package com.example.chessrepertoiretrainer.core.navigation.graph

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.chessrepertoiretrainer.app.AppContainer
import com.example.chessrepertoiretrainer.core.navigation.Screen
import com.example.chessrepertoiretrainer.feature.mygames.presentation.GameDetailScreen
import com.example.chessrepertoiretrainer.feature.mygames.presentation.GameDetailViewModel
import com.example.chessrepertoiretrainer.feature.mygames.presentation.MyGamesScreen
import com.example.chessrepertoiretrainer.feature.mygames.presentation.MyGamesViewModel

fun NavGraphBuilder.myGamesGraph(
    navController: NavHostController,
    appContainer: AppContainer
) {
    composable(Screen.MyGames.route) {
        val vm: MyGamesViewModel = viewModel(
            factory = MyGamesViewModel.Factory(
                repository = appContainer.savedGameRepository,
                syncManager = appContainer.gameSyncManager,
                settingsRepository = appContainer.userSettingsRepository
            )
        )
        MyGamesScreen(
            viewModel = vm,
            onOpenGame = { game ->
                appContainer.latestGame = game
                navController.navigate(Screen.GameDetail.createRoute(game.id))
            }
        )
    }

    composable(
        route = Screen.GameDetail.route,
        arguments = listOf(navArgument("gameId") { type = NavType.StringType })
    ) {
        val game = appContainer.latestGame
        if (game != null) {
            val vm: GameDetailViewModel = viewModel(factory = GameDetailViewModel.Factory(game))
            GameDetailScreen(viewModel = vm, onBackClick = { navController.popBackStack() })
        }
    }
}
