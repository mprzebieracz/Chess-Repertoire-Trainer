package com.example.chessrepertoiretrainer.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import com.example.chessrepertoiretrainer.ui.icons.AppIcons

sealed class Screen(val route: String, val title: String = "", val icon: ImageVector = AppIcons.Home) {
    // Bottom Bar Screens
    object Home : Screen("home", "Home", AppIcons.Home)
    object RepertoireMain : Screen("repertoire_main", "Repertoire", AppIcons.Repertoire)
    object Train : Screen("train", "Train", AppIcons.Train)
    object Puzzles : Screen("puzzles", "Puzzles", AppIcons.Puzzles)
    object YourGames : Screen("games", "Opening tree", AppIcons.OpeningTree)
    object Settings : Screen("settings", "Settings", AppIcons.Settings)

    // Nested / detail screens (not in bottom bar)
    object CourseOverview : Screen("course_overview/{repertoireId}") {
        fun createRoute(repertoireId: Int) = "course_overview/$repertoireId"
    }

    object Chapters : Screen("chapters/{repertoireId}") {
        fun createRoute(repertoireId: Int) = "chapters/$repertoireId"
    }

    // Edit-mode list of lines within a chapter.
    object Lines : Screen("lines/{chapterId}") {
        fun createRoute(chapterId: Int) = "lines/$chapterId"
    }

    // Dedicated learn-mode flow for a chapter (step through each line,
    // then optionally train and finally test the whole chapter).
    object ChapterLearn : Screen("chapter_learn/{chapterId}") {
        fun createRoute(chapterId: Int) = "chapter_learn/$chapterId"
    }

    object LineEditor : Screen("line_editor/{lineId}") {
        fun createRoute(lineId: Int) = "line_editor/$lineId"
    }

    object ChapterTraining : Screen("chapter_training/{chapterId}") {
        fun createRoute(chapterId: Int) = "chapter_training/$chapterId"
    }

    // Training for a single line, reused from the main TrainingViewModel
    // and TrainScreen logic.
    object LineTraining : Screen("line_training/{lineId}") {
        fun createRoute(lineId: Int) = "line_training/$lineId"
    }
    object PuzzleTraining : Screen("puzzle_training")
    object Analysis : Screen("analysis")

    // Opening tree exploration for a specific tracked player profile.
    // Optional query parameters allow filtering which games are included
    // in the tree (e.g. only games as White, by time control, etc.), and
    // limiting how many filtered games are used to build the tree.
    object OpeningTree : Screen("opening_tree/{profileId}?color={color}&timeControl={timeControl}&maxGames={maxGames}") {
        fun createRoute(
            profileId: Long,
            color: String,
            timeControl: String,
            maxGames: Int?
        ): String {
            val safeColor = color.ifBlank { "both" }
            val safeTc = java.net.URLEncoder.encode(timeControl, Charsets.UTF_8.name())
            val safeMax = maxGames?.toString() ?: "-1"
            return "opening_tree/$profileId?color=$safeColor&timeControl=$safeTc&maxGames=$safeMax"
        }
    }
}
