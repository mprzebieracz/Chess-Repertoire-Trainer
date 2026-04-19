package com.example.chessrepertoiretrainer.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.chessrepertoiretrainer.ChessApplication
import com.example.chessrepertoiretrainer.domain.games.GamesRepository
import com.example.chessrepertoiretrainer.ui.viewmodels.SettingsViewModel

@Composable
fun AppNavigation(settingsViewModel: SettingsViewModel) {
    val navController = rememberNavController()
    val appContainer = (LocalContext.current.applicationContext as ChessApplication).appContainer

    val repertoireDao = appContainer.repertoireDao
    val puzzleRepository = appContainer.puzzleRepository
    val playerGamesRepository: GamesRepository = appContainer.gamesRepository

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val showBottomBar = shouldShowBottomBar(navBackStackEntry?.destination)

    Scaffold(
        bottomBar = {
            if (showBottomBar) AppBottomBar(navController)
        }) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            homeGraph(navController, settingsViewModel)
            trainingGraph(navController, repertoireDao)
            gamesGraph(
                navController = navController,
                settingsViewModel = settingsViewModel,
                onlineGamesFetchCoordinator = appContainer.onlineGamesFetchCoordinator,
                openingTreePreparationCoordinator = appContainer.openingTreePreparationCoordinator,
                playerGamesRepository = playerGamesRepository
            )
            puzzlesGraph(navController, puzzleRepository)
            statsGraph(
                navController = navController,
                settingsViewModel = settingsViewModel,
                accountSyncCoordinator = appContainer.accountSyncCoordinator,
                statsRefreshCoordinator = appContainer.statsRefreshCoordinator,
                openingTreePreparationCoordinator = appContainer.openingTreePreparationCoordinator
            )
            repertoireGraph(navController, repertoireDao)
        }
    }
}

