package com.example.chessrepertoiretrainer.core.navigation.graph

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.example.chessrepertoiretrainer.core.navigation.Screen
import com.example.chessrepertoiretrainer.feature.repertoire.domain.RepertoireRepository
import com.example.chessrepertoiretrainer.feature.repertoire.presentation.screen.TrainSelectionScreen
import com.example.chessrepertoiretrainer.feature.repertoire.presentation.viewmodel.TrainingSelectionViewModel

fun NavGraphBuilder.trainingGraph(navController: NavHostController,
                                  repertoireRepository: RepertoireRepository) {
    composable(Screen.Train.route) {
        val vm: TrainingSelectionViewModel =
            viewModel(factory = TrainingSelectionViewModel.Factory(repertoireRepository))
        TrainSelectionScreen(viewModel = vm, onStartTraining = { chapterId ->
            navController.navigate(Screen.ChapterTraining.createRoute(chapterId))
        }, onBackClick = { navController.popBackStack() })
    }
}