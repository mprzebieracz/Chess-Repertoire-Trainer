package com.example.chessrepertoiretrainer.core.navigation.graph

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.example.chessrepertoiretrainer.core.navigation.Screen
import com.example.chessrepertoiretrainer.feature.openingtree.data.OpeningTreePreparationCoordinator
import com.example.chessrepertoiretrainer.feature.settings.presentation.SettingsViewModel
import com.example.chessrepertoiretrainer.feature.stats.data.AccountSyncCoordinator
import com.example.chessrepertoiretrainer.feature.stats.data.StatsRefreshCoordinator
import com.example.chessrepertoiretrainer.feature.stats.presentation.MyStatsScreen
import com.example.chessrepertoiretrainer.feature.stats.presentation.MyStatsViewModel

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