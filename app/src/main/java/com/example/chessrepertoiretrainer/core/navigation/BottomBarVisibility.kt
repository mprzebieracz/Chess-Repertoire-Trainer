package com.example.chessrepertoiretrainer.core.navigation

import androidx.navigation.NavDestination

fun shouldShowBottomBar(destination: NavDestination?): Boolean {
    val route = destination?.route ?: return true
    return when {
        route.startsWith(Screen.LineEditor.route.substringBefore("/")) -> false
        route.startsWith(Screen.ChapterLearn.route.substringBefore("/")) -> false
        route.startsWith(Screen.ChapterReview.route.substringBefore("/")) -> false
        route.startsWith(Screen.LineTraining.route.substringBefore("/")) -> false
        route == Screen.Analysis.route -> false
        route == Screen.PuzzleTraining.route -> false
        route.startsWith(Screen.OpeningTree.route.substringBefore("/")) -> false
        else -> true
    }
}
