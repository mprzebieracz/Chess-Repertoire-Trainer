package com.example.chessrepertoiretrainer.navigation

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.example.chessrepertoiretrainer.ui.screens.AnalysisScreen
import com.example.chessrepertoiretrainer.ui.screens.HomeScreen
import com.example.chessrepertoiretrainer.ui.screens.SettingsScreen
import com.example.chessrepertoiretrainer.ui.viewmodels.AnalysisViewModel
import com.example.chessrepertoiretrainer.ui.viewmodels.SettingsViewModel

fun NavGraphBuilder.homeGraph(
    navController: NavHostController, settingsViewModel: SettingsViewModel
) {
    composable(Screen.Home.route) {
        HomeScreen(
            onOpenAnalysis = { navController.navigate(Screen.Analysis.route) },
            onOpenMyStats = { navController.navigate(Screen.MyStats.route) })
    }

    composable(Screen.Settings.route) {
        SettingsScreen(viewModel = settingsViewModel)
    }

    composable(Screen.Analysis.route) {
        val vm: AnalysisViewModel = viewModel()
        AnalysisScreen(viewModel = vm, onBackClick = { navController.popBackStack() })
    }
}
