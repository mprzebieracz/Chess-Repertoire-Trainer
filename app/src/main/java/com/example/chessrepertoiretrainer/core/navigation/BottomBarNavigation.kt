package com.example.chessrepertoiretrainer.core.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun AppBottomBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val items = listOf(
        Screen.Home,
        Screen.RepertoireMain,
        Screen.Puzzles,
        Screen.OpeningTreeSearch,
        Screen.Settings
    )

    NavigationBar {
        items.forEach { screen ->
            val isRepertoireFlow =
                screen == Screen.RepertoireMain && currentDestination?.hierarchy?.any {
                    it.route?.contains("chapters") == true || it.route?.contains(
                        "lines"
                    ) == true
                } == true

            val selected =
                currentDestination?.hierarchy?.any { it.route == screen.route } == true || isRepertoireFlow

            NavigationBarItem(
                icon = { Icon(screen.icon, contentDescription = screen.title) },
                label = { Text(screen.title) },
                selected = selected,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                })
        }
    }
}