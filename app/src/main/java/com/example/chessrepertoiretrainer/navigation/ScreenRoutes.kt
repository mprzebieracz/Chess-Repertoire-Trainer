package com.example.chessrepertoiretrainer.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Train : Screen("train", "Train", Icons.Default.PlayArrow)
    object Repertoire : Screen("repertoire", "Repertoire", Icons.Default.List)
    object YourGames : Screen("games", "Games", Icons.Default.Person)
    object Analysis : Screen("analysis", "Analysis", Icons.Default.Search)
    object Bluetooth : Screen("bluetooth", "Bluetooth", Icons.Default.Share)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    
    object RepertoireDetail : Screen("repertoire_detail/{repertoireId}", "Chapters", Icons.Default.Info) {
        fun createRoute(repertoireId: Int) = "repertoire_detail/$repertoireId"
    }

    object Lines : Screen("lines/{chapterId}", "Lines", Icons.Default.List) {
        fun createRoute(chapterId: Int) = "lines/$chapterId"
    }

    object LineEditor : Screen("line_editor/{lineId}", "Line Editor", Icons.Default.Edit) {
        fun createRoute(lineId: Int) = "line_editor/$lineId"
    }

    object TrainingSession : Screen("training_session/{chapterId}", "Training", Icons.Default.PlayArrow) {
        fun createRoute(chapterId: Int) = "training_session/$chapterId"
    }
    
    // Legacy - can be removed
    object ChapterEditor : Screen("chapter_editor/{chapterId}", "Chapter Editor", Icons.Default.Edit) {
        fun createRoute(chapterId: Int) = "chapter_editor/$chapterId"
    }
}
