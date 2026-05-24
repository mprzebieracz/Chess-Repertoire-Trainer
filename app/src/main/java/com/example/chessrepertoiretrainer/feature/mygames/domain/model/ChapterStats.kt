package com.example.chessrepertoiretrainer.feature.mygames.domain.model

import com.example.chessrepertoiretrainer.core.database.dao.ChapterStatsRaw

data class ChapterStats(
    val played: Int,
    val wins: Int,
    val losses: Int,
    val draws: Int,
    val gamesInBook: Int,
    val gamesDeviated: Int,
    val avgBookDepthPlies: Double?
) {
    val winPct: Float get() = if (played == 0) 0f else wins.toFloat() / played
    val inBookPct: Float get() = if (played == 0) 0f else gamesInBook.toFloat() / played
    val winPctInBook: Float
        get() {
            if (gamesInBook == 0) return 0f
            // approximation: can't get exact split without a second query, use overall rate
            return winPct
        }
}

fun ChapterStatsRaw.toChapterStats() = ChapterStats(
    played = played,
    wins = wins,
    losses = losses,
    draws = draws,
    gamesInBook = gamesInBook,
    gamesDeviated = gamesDeviated,
    avgBookDepthPlies = avgBookDepthPlies
)
