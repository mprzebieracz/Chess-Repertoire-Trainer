package com.example.chessrepertoiretrainer.feature.mygames.domain.model

import com.example.chessrepertoiretrainer.core.database.dao.GameStatsRaw

val STAT_CATEGORIES = listOf("bullet", "blitz", "rapid", "classical")

data class GameStats(val played: Int, val wins: Int, val losses: Int, val draws: Int) {
    val winPct: Float get() = if (played == 0) 0f else wins.toFloat() / played
    val lossPct: Float get() = if (played == 0) 0f else losses.toFloat() / played
    val drawPct: Float get() = if (played == 0) 0f else draws.toFloat() / played
}

fun GameStatsRaw.toGameStats() = GameStats(played, wins, losses, draws)

data class CategoryStats(
    val category: String,
    val currentRating: Int?,
    val ratingDiff: Int?,
    val peakRating: Int?,
    val peakRatingDate: Long?,
    val avgOpponentRating: Int?,
    val allStats: GameStats,
    val whiteStats: GameStats,
    val blackStats: GameStats,
    val isExpanded: Boolean = false
)