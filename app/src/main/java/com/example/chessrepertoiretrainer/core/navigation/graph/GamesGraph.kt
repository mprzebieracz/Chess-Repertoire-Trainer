package com.example.chessrepertoiretrainer.core.navigation.graph

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.chessrepertoiretrainer.core.navigation.Screen
import com.example.chessrepertoiretrainer.feature.openingtree.data.OpeningTreePreparationCoordinator
import com.example.chessrepertoiretrainer.feature.openingtree.data.fetcher.OnlineGamesFetchCoordinator
import com.example.chessrepertoiretrainer.feature.openingtree.presentation.OpeningTreeScreen
import com.example.chessrepertoiretrainer.feature.openingtree.presentation.OpeningTreeSearchScreen
import com.example.chessrepertoiretrainer.feature.openingtree.presentation.OpeningTreeSearchViewModel
import com.example.chessrepertoiretrainer.feature.openingtree.presentation.OpeningTreeViewModel
import com.example.chessrepertoiretrainer.feature.settings.presentation.SettingsViewModel

fun NavGraphBuilder.gamesGraph(
    navController: NavHostController,
    settingsViewModel: SettingsViewModel,
    onlineGamesFetchCoordinator: OnlineGamesFetchCoordinator,
    openingTreePreparationCoordinator: OpeningTreePreparationCoordinator
) {
    composable(Screen.OpeningTreeSearch.route) {
        val vm: OpeningTreeSearchViewModel = viewModel(
            factory = OpeningTreeSearchViewModel.Factory(
                onlineGamesFetchCoordinator, openingTreePreparationCoordinator
            )
        )

        val settings = settingsViewModel.settings.collectAsStateWithLifecycle().value

        OpeningTreeSearchScreen(
            viewModel = vm,
            defaultLichessUsername = settings.lichessUsername,
            defaultChessComUsername = settings.chessComUsername,
            defaultPlatform = settings.defaultOnlinePlatform,
            onOpenTree = { username, platform, color, timeControl, maxGames ->
                navController.navigate(
                    Screen.OpeningTree.createRoute(
                        username = username,
                        platform = platform,
                        color = color,
                        timeControl = timeControl,
                        maxGames = maxGames
                    )
                )
            })
    }

    composable(
        route = Screen.OpeningTree.route,
        arguments = listOf(
            navArgument("username") { type = NavType.StringType },
            navArgument("platform") { type = NavType.StringType },
            navArgument("color") { type = NavType.StringType; defaultValue = "both" },
            navArgument("timeControl") { type = NavType.StringType; defaultValue = "none" },
            navArgument("maxGames") {
                type = NavType.IntType; defaultValue = -1
            })
    ) { backStackEntry ->
        val username = backStackEntry.arguments?.getString("username") ?: return@composable
        val platform = backStackEntry.arguments?.getString("platform") ?: return@composable
        val colorArg = backStackEntry.arguments?.getString("color") ?: "both"
        val timeControlArg = backStackEntry.arguments?.getString("timeControl") ?: "none"
        val maxGamesArg = backStackEntry.arguments?.getInt("maxGames") ?: -1

        val maxGamesForTree = maxGamesArg.takeIf { it > 0 }
        val finalTimeControl = if (timeControlArg == "none") "" else timeControlArg

        val colorFilter = when (colorArg.lowercase()) {
            "white" -> OpeningTreeViewModel.ColorFilter.WHITE_ONLY
            "black" -> OpeningTreeViewModel.ColorFilter.BLACK_ONLY
            else -> OpeningTreeViewModel.ColorFilter.BOTH
        }

        val vm: OpeningTreeViewModel = viewModel(
            factory = OpeningTreeViewModel.Factory(
                username = username,
                platform = platform,
                openingTreePreparationCoordinator = openingTreePreparationCoordinator,
                colorFilter = colorFilter,
                timeControlFilter = finalTimeControl.ifBlank { null },
                maxGamesForTree = maxGamesForTree
            )
        )

        OpeningTreeScreen(
            viewModel = vm, onBackClick = { navController.popBackStack() })
    }
}