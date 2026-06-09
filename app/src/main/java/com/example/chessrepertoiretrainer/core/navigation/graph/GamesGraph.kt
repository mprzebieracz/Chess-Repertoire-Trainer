package com.example.chessrepertoiretrainer.core.navigation.graph

import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.example.chessrepertoiretrainer.app.AppContainer
import com.example.chessrepertoiretrainer.core.navigation.Screen
import com.example.chessrepertoiretrainer.feature.openingtree.presentation.MasterGameViewerScreen
import com.example.chessrepertoiretrainer.feature.openingtree.presentation.MasterGameViewerViewModel
import com.example.chessrepertoiretrainer.feature.openingtree.presentation.OpeningExplorerScreen
import com.example.chessrepertoiretrainer.feature.openingtree.presentation.OpeningExplorerViewModel
import com.example.chessrepertoiretrainer.feature.openingtree.presentation.OpeningHubScreen
import com.example.chessrepertoiretrainer.feature.openingtree.presentation.OpeningTreeScreen
import com.example.chessrepertoiretrainer.feature.openingtree.presentation.OpeningTreeSearchScreen
import com.example.chessrepertoiretrainer.feature.openingtree.presentation.OpeningTreeSearchViewModel
import com.example.chessrepertoiretrainer.feature.openingtree.presentation.OpeningTreeViewModel
import com.example.chessrepertoiretrainer.feature.settings.presentation.SettingsViewModel

fun NavGraphBuilder.gamesGraph(
    navController: NavHostController,
    settingsViewModel: SettingsViewModel,
    appContainer: AppContainer
) {
    // Hub: two options — Opening Tree (from your games) or Opening Explorer (Lichess data)
    composable(Screen.OpeningTreeSearch.route) {
        OpeningHubScreen(
            onOpenTree = { navController.navigate("openingtree_search_form") },
            onOpenExplorer = { navController.navigate(Screen.OpeningExplorer.route) }
        )
    }

    // Opening Tree filter form (search your games, build tree)
    composable("openingtree_search_form") {
        val vm: OpeningTreeSearchViewModel =
            viewModel(factory = OpeningTreeSearchViewModel.Factory(appContainer.gameFetcherRegistry))
        val settings = settingsViewModel.settings.collectAsStateWithLifecycle().value
        OpeningTreeSearchScreen(
            viewModel = vm,
            defaultLichessUsername = settings.lichessUsername,
            defaultChessComUsername = settings.chessComUsername,
            defaultPlatform = settings.defaultOnlinePlatform,
            onOpenTree = { tree ->
                appContainer.navTransientStore.openingTree = tree
                navController.navigate(Screen.OpeningTree.route)
            },
            onBackClick = { navController.popBackStack() }
        )
    }

    // Opening Tree board view (local games tree)
    composable(Screen.OpeningTree.route) {
        val tree = remember { appContainer.navTransientStore.takeOpeningTree() }
        val vm: OpeningTreeViewModel = viewModel(factory = OpeningTreeViewModel.Factory(tree))
        OpeningTreeScreen(viewModel = vm, onBackClick = { navController.popBackStack() })
    }

    // Lichess Opening Explorer (Players + Masters)
    composable(Screen.OpeningExplorer.route) {
        val startFen = remember { appContainer.navTransientStore.takeExplorerStartFen() }
        val vm: OpeningExplorerViewModel = viewModel(
            factory = OpeningExplorerViewModel.Factory(
                appContainer.lichessExplorerService,
                startFen
            )
        )
        OpeningExplorerScreen(
            viewModel = vm,
            onBackClick = { navController.popBackStack() },
            onOpenMasterGame = { pgn ->
                appContainer.navTransientStore.masterGamePgn = pgn
                navController.navigate(Screen.MasterGameViewer.route)
            }
        )
    }

    composable(Screen.MasterGameViewer.route) {
        val pgn = remember { appContainer.navTransientStore.takeMasterGamePgn() }
        if (pgn != null) {
            val vm: MasterGameViewerViewModel =
                viewModel(factory = MasterGameViewerViewModel.Factory(pgn))
            MasterGameViewerScreen(viewModel = vm, onBackClick = { navController.popBackStack() })
        }
    }
}