package com.example.chessrepertoiretrainer.core.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import com.example.chessrepertoiretrainer.core.ui.icons.AppIcons
import java.net.URLEncoder

sealed class Screen(
    val route: String, val title: String = "", val icon: ImageVector = AppIcons.Home
) {
    // Bottom Bar Screens
    object Home : Screen("home", "Home", AppIcons.Home)
    object RepertoireMain : Screen("repertoire_main", "Repertoire", AppIcons.Repertoire)

    object Train : Screen("train", "Train", AppIcons.Train)
    object Puzzles : Screen("puzzles", "Puzzles", AppIcons.Puzzles)

    object YourGames : Screen("games", "Openingtree", AppIcons.OpeningTree)
    object Settings : Screen("settings", "Settings", AppIcons.Settings)

    // Other Screens

    object CourseOverview : Screen("course_overview/{repertoireId}") {
        fun createRoute(repertoireId: Int) = "course_overview/$repertoireId"
    }

    object Chapters : Screen("chapters/{repertoireId}") {
        fun createRoute(repertoireId: Int) = "chapters/$repertoireId"
    }

    object Lines : Screen("lines/{chapterId}") {
        fun createRoute(chapterId: Int) = "lines/$chapterId"
    }

    object ChapterLearn : Screen("chapter_learn/{chapterId}") {
        fun createRoute(chapterId: Int) = "chapter_learn/$chapterId"
    }

    object ChapterReview : Screen("chapter_review/{chapterId}") {
        fun createRoute(chapterId: Int) = "chapter_review/$chapterId"
    }

    object LineEditor : Screen("line_editor/{lineId}") {
        fun createRoute(lineId: Int) = "line_editor/$lineId"
    }

    object ChapterTraining : Screen("chapter_training/{chapterId}") {
        fun createRoute(chapterId: Int) = "chapter_training/$chapterId"
    }

    object LineTraining : Screen("line_training/{lineId}") {
        fun createRoute(lineId: Int) = "line_training/$lineId"
    }

    object PuzzleTraining : Screen("puzzle_training")
    object Analysis : Screen("analysis")

    object OpeningTree :
        Screen("opening_tree/{profileId}?color={color}&timeControl={timeControl}&maxGames={maxGames}") {
        fun createRoute(
            profileId: Long, color: String, timeControl: String, maxGames: Int?
        ): String {
            val safeColor = color.ifBlank { "both" }
            val safeTc = URLEncoder.encode(timeControl, Charsets.UTF_8.name())
            val safeMax = maxGames?.toString() ?: "-1"
            return "opening_tree/$profileId?color=$safeColor&timeControl=$safeTc&maxGames=$safeMax"
        }
    }
}
