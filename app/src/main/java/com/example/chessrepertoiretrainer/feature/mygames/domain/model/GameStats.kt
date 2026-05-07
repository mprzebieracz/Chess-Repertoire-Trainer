package com.example.chessrepertoiretrainer.feature.mygames.domain.model

import com.example.chessrepertoiretrainer.core.database.dao.GameStatsRaw

data class GameStats(
    val played: Int,
    val wins: Int,
    val losses: Int,
    val draws: Int
) {
    val winPct: Float get() = if (played == 0) 0f else wins.toFloat() / played
    val lossPct: Float get() = if (played == 0) 0f else losses.toFloat() / played
    val drawPct: Float get() = if (played == 0) 0f else draws.toFloat() / played
}

fun GameStatsRaw.toGameStats() = GameStats(played, wins, losses, draws)

val CATEGORIES = listOf("all", "bullet", "blitz", "rapid", "classical")
