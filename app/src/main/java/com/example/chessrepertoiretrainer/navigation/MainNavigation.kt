package com.example.chessrepertoiretrainer.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.example.chessrepertoiretrainer.ChessApplication
import com.example.chessrepertoiretrainer.ui.screens.*
import com.example.chessrepertoiretrainer.ui.viewmodels.RepertoireViewModel
import com.example.chessrepertoiretrainer.ui.viewmodels.RepertoireDetailViewModel
import com.example.chessrepertoiretrainer.ui.viewmodels.LinesViewModel
import com.example.chessrepertoiretrainer.ui.viewmodels.LineEditorViewModel
import com.example.chessrepertoiretrainer.ui.viewmodels.TrainingViewModel

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val application = context.applicationContext as ChessApplication
    val repertoireDao = application.database.repertoireDao()
    
    val repertoireViewModel: RepertoireViewModel = viewModel(
        factory = RepertoireViewModel.Factory(repertoireDao)
    )

    Scaffold(
        bottomBar = { BottomNavigationBar(navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) { HomeScreen() }
            composable(Screen.Train.route) { 
                TrainScreen(
                    viewModel = repertoireViewModel,
                    onRepertoireClick = { id ->
                        navController.navigate(Screen.RepertoireDetail.createRoute(id))
                    }
                ) 
            }
            composable(Screen.Repertoire.route) { 
                RepertoireScreen(
                    viewModel = repertoireViewModel,
                    onRepertoireClick = { id -> 
                        navController.navigate(Screen.RepertoireDetail.createRoute(id))
                    }
                ) 
            }
            composable(Screen.YourGames.route) { YourGamesScreen() }
            composable(Screen.Analysis.route) { AnalysisScreen() }
            composable(Screen.Bluetooth.route) { BluetoothScreen() }
            composable(Screen.Settings.route) { SettingsScreen() }
            
            composable(Screen.RepertoireDetail.route) { backStackEntry ->
                val detailViewModel: RepertoireDetailViewModel = viewModel(
                    factory = RepertoireDetailViewModel.Factory(
                        repertoireDao = repertoireDao,
                        owner = backStackEntry,
                        defaultArgs = backStackEntry.arguments
                    )
                )
                RepertoireDetailScreen(
                    viewModel = detailViewModel,
                    onChapterClick = { id ->
                        navController.navigate(Screen.Lines.createRoute(id))
                    },
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(Screen.Lines.route) { backStackEntry ->
                val linesViewModel: LinesViewModel = viewModel(
                    factory = LinesViewModel.Factory(
                        repertoireDao = repertoireDao,
                        owner = backStackEntry,
                        defaultArgs = backStackEntry.arguments
                    )
                )
                LinesScreen(
                    viewModel = linesViewModel,
                    onLineClick = { id ->
                        navController.navigate(Screen.LineEditor.createRoute(id))
                    },
                    onTrainChapterClick = { id ->
                        navController.navigate(Screen.TrainingSession.createRoute(id))
                    },
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(Screen.LineEditor.route) { backStackEntry ->
                val editorViewModel: LineEditorViewModel = viewModel(
                    factory = LineEditorViewModel.Factory(
                        repertoireDao = repertoireDao,
                        owner = backStackEntry,
                        defaultArgs = backStackEntry.arguments
                    )
                )
                LineEditorScreen(
                    viewModel = editorViewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(Screen.TrainingSession.route) { backStackEntry ->
                val trainingViewModel: TrainingViewModel = viewModel(
                    factory = TrainingViewModel.Factory(
                        repertoireDao = repertoireDao,
                        owner = backStackEntry,
                        defaultArgs = backStackEntry.arguments
                    )
                )
                TrainingSessionScreen(
                    viewModel = trainingViewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val items = listOf(
        Screen.Home, Screen.Train, Screen.Repertoire,
        Screen.YourGames, Screen.Analysis, Screen.Bluetooth, Screen.Settings
    )

    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { screen ->
            NavigationBarItem(
                icon = { Icon(screen.icon, contentDescription = screen.title) },
                label = { Text(screen.title) },
                selected = currentRoute == screen.route,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}
