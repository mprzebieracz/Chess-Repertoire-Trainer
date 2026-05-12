package com.example.chessrepertoiretrainer.core.navigation.graph

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.chessrepertoiretrainer.app.AppContainer
import com.example.chessrepertoiretrainer.core.navigation.Screen
import com.example.chessrepertoiretrainer.feature.repertoire.domain.RepertoireRepository
import com.example.chessrepertoiretrainer.feature.repertoire.domain.usecase.RepertoireComplianceAnalyzer
import com.example.chessrepertoiretrainer.feature.repertoire.presentation.screen.CourseOverviewScreen
import com.example.chessrepertoiretrainer.feature.repertoire.presentation.screen.EditChapterScreen
import com.example.chessrepertoiretrainer.feature.repertoire.presentation.screen.EditCourseScreen
import com.example.chessrepertoiretrainer.feature.repertoire.presentation.screen.LearnChapterScreen
import com.example.chessrepertoiretrainer.feature.repertoire.presentation.screen.LineEditorScreen
import com.example.chessrepertoiretrainer.feature.repertoire.presentation.screen.RepertoiresScreen
import com.example.chessrepertoiretrainer.feature.repertoire.presentation.screen.ReviewChapterScreen
import com.example.chessrepertoiretrainer.feature.repertoire.presentation.screen.TrainScreen
import com.example.chessrepertoiretrainer.feature.repertoire.presentation.viewmodel.CourseOverviewViewModel
import com.example.chessrepertoiretrainer.feature.repertoire.presentation.viewmodel.EditChapterViewModel
import com.example.chessrepertoiretrainer.feature.repertoire.presentation.viewmodel.EditCourseViewModel
import com.example.chessrepertoiretrainer.feature.repertoire.presentation.viewmodel.LearnChapterViewModel
import com.example.chessrepertoiretrainer.feature.repertoire.presentation.viewmodel.LineEditorViewModel
import com.example.chessrepertoiretrainer.feature.repertoire.presentation.viewmodel.RepertoiresViewModel
import com.example.chessrepertoiretrainer.feature.repertoire.presentation.viewmodel.ReviewChapterViewModel
import com.example.chessrepertoiretrainer.feature.repertoire.presentation.viewmodel.TrainingViewModel

fun NavGraphBuilder.repertoireGraph(
    navController: NavHostController,
    repertoireRepository: RepertoireRepository,
    complianceAnalyzer: RepertoireComplianceAnalyzer,
    appContainer: AppContainer
) {
    composable(Screen.RepertoireMain.route) {
        val vm: RepertoiresViewModel = viewModel(
            factory = RepertoiresViewModel.Factory(
                repertoireRepository,
                complianceAnalyzer
            )
        )
        RepertoiresScreen(viewModel = vm, onNavigateToChapters = {
            navController.navigate(Screen.CourseOverview.createRoute(it))
        })
    }

    composable(
        Screen.CourseOverview.route,
        arguments = listOf(navArgument("repertoireId") { type = NavType.IntType })
    ) {
        val vm: CourseOverviewViewModel =
            viewModel(factory = CourseOverviewViewModel.Factory(repertoireRepository))
        CourseOverviewScreen(
            viewModel = vm,
            onBackClick = { navController.popBackStack() },
            onEditCourse = {
                navController.navigate(Screen.EditCourse.createRoute(it))
            },
            onOpenChapterLearn = {
                navController.navigate(Screen.ChapterLearn.createRoute(it))
            },
            onOpenChapterTrain = {
                navController.navigate(Screen.ChapterTraining.createRoute(it))
            },
            onOpenChapterReview = {
                navController.navigate(Screen.ChapterReview.createRoute(it))
            },
            onStartMultiChapterTraining = { chapterIds ->
                appContainer.navTransientStore.selectedChapterIds = chapterIds
                navController.navigate(Screen.MultiChapterTraining.route)
            })
    }

    composable(
        Screen.EditCourse.route,
        arguments = listOf(navArgument("repertoireId") { type = NavType.IntType })
    ) {
        val vm: EditCourseViewModel =
            viewModel(factory = EditCourseViewModel.Factory(repertoireRepository))
        EditCourseScreen(viewModel = vm, onNavigateToEditChapter = {
            navController.navigate(Screen.EditChapter.createRoute(it))
        }, onCourseDeleted = {
            navController.popBackStack(Screen.RepertoireMain.route, false)
        }, onBackClick = { navController.popBackStack() })
    }

    composable(
        Screen.EditChapter.route,
        arguments = listOf(navArgument("chapterId") { type = NavType.IntType })
    ) {
        val vm: EditChapterViewModel =
            viewModel(factory = EditChapterViewModel.Factory(repertoireRepository))
        EditChapterScreen(viewModel = vm, onNavigateToLineEditor = {
            navController.navigate(Screen.LineEditor.createRoute(it))
        }, onBackClick = { navController.popBackStack() })
    }

    composable(Screen.ChapterLearn.route, arguments = listOf(navArgument("chapterId") {
        type = NavType.IntType
    })) { backStackEntry ->
        val vm: LearnChapterViewModel =
            viewModel(factory = LearnChapterViewModel.Factory(repertoireRepository))
        val savedStateHandle = backStackEntry.savedStateHandle
        val lineTrainingFinished by savedStateHandle.getStateFlow("lineTrainingFinished", false)
            .collectAsStateWithLifecycle()
        LaunchedEffect(lineTrainingFinished) {
            if (lineTrainingFinished) {
                vm.onLineTrainingFinished(); savedStateHandle["lineTrainingFinished"] = false
            }
        }
        LearnChapterScreen(
            viewModel = vm,
            onBackClick = { navController.popBackStack() },
            onStartChapterTraining = {
                navController.navigate(Screen.ChapterTraining.createRoute(it))
            },
            onStartLineTraining = {
                navController.navigate(Screen.LineTraining.createRoute(it))
            },
            onOpenInAnalysis = { fen ->
                appContainer.navTransientStore.analysisStartFen = fen
                navController.navigate(Screen.Analysis.route)
            })
    }

    composable(
        Screen.ChapterReview.route,
        arguments = listOf(
            navArgument("chapterId") { type = NavType.IntType },
            navArgument("startLineId") {
                type = NavType.IntType; defaultValue = -1
            })
    ) {
        val vm: ReviewChapterViewModel = viewModel(
            factory = ReviewChapterViewModel.Factory(
                repertoireRepository,
                appContainer.stockfishEngine
            )
        )
        ReviewChapterScreen(viewModel = vm, onBackClick = { navController.popBackStack() })
    }

    composable(
        Screen.LineEditor.route,
        arguments = listOf(navArgument("lineId") { type = NavType.IntType })
    ) {
        val vm: LineEditorViewModel = viewModel(
            factory = LineEditorViewModel.Factory(
                repertoireRepository,
                appContainer.stockfishEngine
            )
        )
        LineEditorScreen(viewModel = vm, onBackClick = { navController.popBackStack() })
    }

    composable(
        Screen.ChapterTraining.route,
        arguments = listOf(navArgument("chapterId") { type = NavType.IntType })
    ) {
        val vm: TrainingViewModel =
            viewModel(factory = TrainingViewModel.Factory(repertoireRepository))
        TrainScreen(viewModel = vm, onBackClick = { navController.popBackStack() })
    }

    composable(
        Screen.LineTraining.route,
        arguments = listOf(navArgument("lineId") { type = NavType.IntType })
    ) {
        val vm: TrainingViewModel =
            viewModel(factory = TrainingViewModel.Factory(repertoireRepository))
        TrainScreen(
            viewModel = vm,
            onBackClick = { navController.popBackStack() },
            onSessionComplete = {
                navController.previousBackStackEntry?.savedStateHandle?.set(
                    "lineTrainingFinished",
                    true
                ); navController.popBackStack()
            })
    }

    composable(Screen.MultiChapterTraining.route) {
        val vm: TrainingViewModel =
            viewModel(factory = TrainingViewModel.Factory(repertoireRepository, appContainer))
        appContainer.navTransientStore.selectedChapterIds = null
        TrainScreen(viewModel = vm, onBackClick = { navController.popBackStack() })
    }
}