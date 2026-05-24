package com.example.chessrepertoiretrainer.core.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import com.example.chessrepertoiretrainer.core.ui.icons.AppIcons

sealed class Screen(
    val route: String,
    val title: String = "",
    val icon: ImageVector = AppIcons.Home
) {
    // Bottom Bar Screens
    object Home : Screen("home", "Home", AppIcons.Home)
    object RepertoireMain : Screen("repertoire_main", "Repertoire", AppIcons.Repertoire)

    object Train : Screen("train", "Train", AppIcons.Train)
    object MyGames : Screen("my_games", "Games", AppIcons.MyGames)

    object OpeningTreeSearch : Screen("games", "Openingtree", AppIcons.OpeningTree)
    object Settings : Screen("settings", "Settings", AppIcons.Settings)

    // Other Screens

    object CourseOverview : Screen("course_overview/{repertoireId}") {
        fun createRoute(repertoireId: Int) = "course_overview/$repertoireId"
    }

    object ChapterLearn : Screen("chapter_learn/{chapterId}") {
        fun createRoute(chapterId: Int) = "chapter_learn/$chapterId"
    }

    object ChapterReview : Screen("chapter_review/{chapterId}?startLineId={startLineId}") {
        fun createRoute(chapterId: Int, startLineId: Int? = null): String {
            val base = "chapter_review/$chapterId"
            return if (startLineId != null) "$base?startLineId=$startLineId" else base
        }
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
    object GameDetail : Screen("game_detail/{gameId}") {
        fun createRoute(gameId: String) = "game_detail/${android.net.Uri.encode(gameId)}"
    }

    object AccountStats : Screen("account_stats/{platform}/{username}") {
        fun createRoute(platform: String, username: String) =
            "account_stats/${android.net.Uri.encode(platform)}/${android.net.Uri.encode(username)}"
    }

    object GamesList : Screen("games_list")

    object OpeningTree : Screen("opening_tree")

    object EditCourse : Screen("edit_course/{repertoireId}") {
        fun createRoute(repertoireId: Int) = "edit_course/$repertoireId"
    }

    object EditChapter : Screen("edit_chapter/{chapterId}") {
        fun createRoute(chapterId: Int) = "edit_chapter/$chapterId"
    }

    object MultiChapterTraining : Screen("multi_chapter_training")

    object MasterGameViewer : Screen("master_game_viewer")

    object OpeningExplorer : Screen("opening_explorer")

    object RepertoirePuzzles : Screen("repertoire_puzzles")
    object OpeningPuzzleSession : Screen("opening_puzzle_session")

    object OpeningStats : Screen("opening_stats/{platform}/{username}") {
        fun createRoute(platform: String, username: String) =
            "opening_stats/${android.net.Uri.encode(platform)}/${android.net.Uri.encode(username)}"
    }

    object CourseTransfer : Screen("course_transfer")
}