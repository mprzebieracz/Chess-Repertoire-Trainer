package com.example.chessrepertoiretrainer.core.navigation.graph

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.example.chessrepertoiretrainer.app.AppContainer
import com.example.chessrepertoiretrainer.core.navigation.Screen
import com.example.chessrepertoiretrainer.feature.openingtree.presentation.OpeningTreeScreen
import com.example.chessrepertoiretrainer.feature.openingtree.presentation.OpeningTreeSearchScreen
import com.example.chessrepertoiretrainer.feature.openingtree.presentation.OpeningTreeSearchViewModel
import com.example.chessrepertoiretrainer.feature.openingtree.presentation.OpeningTreeViewModel
import com.example.chessrepertoiretrainer.feature.settings.presentation.SettingsViewModel

fun NavGraphBuilder.gamesGraph(
    navController: NavHostController,
    settingsViewModel: SettingsViewModel,
    appContainer: AppContainer
) {
    composable(Screen.OpeningTreeSearch.route) {
        val vm: OpeningTreeSearchViewModel = viewModel(
            factory = OpeningTreeSearchViewModel.Factory(appContainer.gameFetcherRegistry)
        )

        val settings = settingsViewModel.settings.collectAsStateWithLifecycle().value

        OpeningTreeSearchScreen(
            viewModel = vm,
            defaultLichessUsername = settings.lichessUsername,
            defaultChessComUsername = settings.chessComUsername,
            defaultPlatform = settings.defaultOnlinePlatform,
            onOpenTree = { tree ->
                appContainer.latestOpeningTree = tree
                navController.navigate(Screen.OpeningTree.route)
            }
        )
    }

    composable(Screen.OpeningTree.route) {
        val vm: OpeningTreeViewModel = viewModel(
            factory = OpeningTreeViewModel.Factory(appContainer.latestOpeningTree)
        )

        OpeningTreeScreen(
            viewModel = vm,
            onBackClick = { navController.popBackStack() }
        )
    }
}
