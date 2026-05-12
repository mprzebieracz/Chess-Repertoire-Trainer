package com.example.chessrepertoiretrainer.core.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.chessrepertoiretrainer.app.ChessApplication
import com.example.chessrepertoiretrainer.core.navigation.graph.gamesGraph
import com.example.chessrepertoiretrainer.core.navigation.graph.homeGraph
import com.example.chessrepertoiretrainer.core.navigation.graph.myGamesGraph
import com.example.chessrepertoiretrainer.core.navigation.graph.repertoireGraph
import com.example.chessrepertoiretrainer.core.navigation.graph.trainingGraph
import com.example.chessrepertoiretrainer.feature.settings.presentation.SettingsViewModel

@Composable
fun AppNavigation(settingsViewModel: SettingsViewModel) {
    val navController = rememberNavController()
    val appContainer = (LocalContext.current.applicationContext as ChessApplication).appContainer

    val repertoireRepository = appContainer.repertoireRepository

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val showBottomBar = shouldShowBottomBar(navBackStackEntry?.destination)

    Scaffold(bottomBar = {
        if (showBottomBar) AppBottomBar(navController)
    }) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            homeGraph(navController, settingsViewModel, appContainer.puzzleRepository, appContainer)
            trainingGraph(navController, repertoireRepository)
            gamesGraph(
                navController = navController,
                settingsViewModel = settingsViewModel,
                appContainer = appContainer
            )
            myGamesGraph(navController, appContainer)
            repertoireGraph(
                navController,
                repertoireRepository,
                appContainer.repertoireComplianceAnalyzer,
                appContainer
            )
        }
    }
}