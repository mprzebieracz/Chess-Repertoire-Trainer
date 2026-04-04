package com.example.chessrepertoiretrainer.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String = "", val icon: ImageVector = Icons.Default.Home) {
    // Bottom Bar Screens
    object Home : Screen("home", "Home", Icons.Default.Home)
    object RepertoireMain : Screen("repertoire_main", "Repertoire", Icons.AutoMirrored.Filled.List)
    object Train : Screen("train", "Train", Icons.Default.PlayArrow)
    object Puzzles : Screen("puzzles", "Puzzles", Icons.Default.PlayArrow)
    object YourGames : Screen("games", "Games", Icons.Default.Person)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)

    // Nested / detail screens (not in bottom bar)
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
    object PuzzleTraining : Screen("puzzle_training")
    object Analysis : Screen("analysis")
}
