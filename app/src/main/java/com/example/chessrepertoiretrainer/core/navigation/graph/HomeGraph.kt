package com.example.chessrepertoiretrainer.core.navigation.graph

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.example.chessrepertoiretrainer.core.navigation.Screen
import com.example.chessrepertoiretrainer.feature.analysis.AnalysisScreen
import com.example.chessrepertoiretrainer.feature.analysis.AnalysisViewModel
import com.example.chessrepertoiretrainer.feature.home.HomeScreen
import com.example.chessrepertoiretrainer.feature.settings.presentation.SettingsScreen
import com.example.chessrepertoiretrainer.feature.settings.presentation.SettingsViewModel

fun NavGraphBuilder.homeGraph(
    navController: NavHostController,
    settingsViewModel: SettingsViewModel
) {
    composable(Screen.Home.route) {
        HomeScreen(
            onOpenAnalysis = { navController.navigate(Screen.Analysis.route) },
            onOpenMyStats = { navController.navigate(Screen.MyStats.route) }
        )
    }

    composable(Screen.Settings.route) {
        SettingsScreen(viewModel = settingsViewModel)
    }

    composable(Screen.Analysis.route) {
        val vm: AnalysisViewModel = viewModel()
        AnalysisScreen(viewModel = vm, onBackClick = { navController.popBackStack() })
    }
}
