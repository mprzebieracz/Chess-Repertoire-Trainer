package com.example.chessrepertoiretrainer.core.navigation

import androidx.navigation.NavDestination

private val hiddenRouteExact = setOf(
    Screen.MultiChapterTraining.route,
    Screen.Analysis.route,
    Screen.PuzzleTraining.route,
    Screen.GamesList.route,
)

private val hiddenRoutePrefixes = listOf(
    Screen.LineEditor,
    Screen.ChapterLearn,
    Screen.ChapterReview,
    Screen.LineTraining,
    Screen.EditCourse,
    Screen.EditChapter,
    Screen.OpeningTree,
    Screen.GameDetail,
    Screen.AccountStats,
).map { it.route.substringBefore("/") }

fun shouldShowBottomBar(destination: NavDestination?): Boolean {
    val route = destination?.route ?: return true
    return route !in hiddenRouteExact && hiddenRoutePrefixes.none { route.startsWith(it) }
}