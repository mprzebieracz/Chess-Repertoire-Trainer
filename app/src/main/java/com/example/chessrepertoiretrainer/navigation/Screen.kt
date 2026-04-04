package com.example.chessrepertoiretrainer.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String = "", val icon: ImageVector = Icons.Default.Home) {
    // Bottom Bar Screens
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Train : Screen("train", "Train", Icons.Default.PlayArrow)
    object RepertoireMain : Screen("repertoire_main", "Repertoire", Icons.AutoMirrored.Filled.List)
    object YourGames : Screen("games", "Games", Icons.Default.Person)
    object Analysis : Screen("analysis", "Analysis", Icons.Default.Search)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)

    // Nested Repertoire Screens (Not in Bottom Bar)
    object Chapters : Screen("chapters/{repertoireId}") {
        fun createRoute(repertoireId: Int) = "chapters/$repertoireId"
    }
    object Lines : Screen("lines/{chapterId}") {
        fun createRoute(chapterId: Int) = "lines/$chapterId"
    }
    object LineEditor : Screen("line_editor/{lineId}") {
        fun createRoute(lineId: Int) = "line_editor/$lineId"
    }
    object ChapterTraining : Screen("chapter_training/{chapterId}") {
        fun createRoute(chapterId: Int) = "chapter_training/$chapterId"
    }
}
