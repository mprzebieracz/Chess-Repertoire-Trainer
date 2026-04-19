package com.example.chessrepertoiretrainer.navigation

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.example.chessrepertoiretrainer.database.dao.RepertoireDao
import com.example.chessrepertoiretrainer.ui.screens.TrainSelectionScreen
import com.example.chessrepertoiretrainer.ui.viewmodels.TrainingSelectionViewModel

fun NavGraphBuilder.trainingGraph(
    navController: NavHostController, repertoireDao: RepertoireDao
) {
    composable(Screen.Train.route) {
        val vm: TrainingSelectionViewModel =
            viewModel(factory = TrainingSelectionViewModel.Factory(repertoireDao))
        TrainSelectionScreen(viewModel = vm, onStartTraining = { chapterId ->
            navController.navigate(Screen.ChapterTraining.createRoute(chapterId))
        }, onBackClick = { navController.popBackStack() })
    }
}
