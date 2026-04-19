package com.example.chessrepertoiretrainer.navigation

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.example.chessrepertoiretrainer.data.AccountSyncCoordinator
import com.example.chessrepertoiretrainer.data.OpeningTreePreparationCoordinator
import com.example.chessrepertoiretrainer.data.StatsRefreshCoordinator
import com.example.chessrepertoiretrainer.ui.screens.MyStatsScreen
import com.example.chessrepertoiretrainer.ui.viewmodels.MyStatsViewModel
import com.example.chessrepertoiretrainer.ui.viewmodels.SettingsViewModel

fun NavGraphBuilder.statsGraph(
    navController: NavHostController,
    settingsViewModel: SettingsViewModel,
    accountSyncCoordinator: AccountSyncCoordinator,
    statsRefreshCoordinator: StatsRefreshCoordinator,
    openingTreePreparationCoordinator: OpeningTreePreparationCoordinator
) {
    composable(Screen.MyStats.route) {
        val vm: MyStatsViewModel = viewModel(
            factory = MyStatsViewModel.Factory(
                accountSyncCoordinator = accountSyncCoordinator,
                statsRefreshCoordinator = statsRefreshCoordinator,
                openingTreePreparationCoordinator = openingTreePreparationCoordinator
            )
        )

        val settings = settingsViewModel.settings.collectAsStateWithLifecycle().value

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
            })
    }
}
