package com.example.chessrepertoiretrainer.feature.mygames.domain.model

import com.example.chessrepertoiretrainer.core.database.dao.OpeningStatsRaw

data class OpeningStats(
    val ecoCode: String,
    val openingName: String,
    val family: String,
    val played: Int,
    val wins: Int,
    val losses: Int,
    val draws: Int,
    val gamesInBook: Int,
    val gamesDeviated: Int
) {
    val winPct: Float get() = if (played == 0) 0f else wins.toFloat() / played
    val inBookPct: Float get() = if (played == 0) 0f else gamesInBook.toFloat() / played
}

data class OpeningFamilyGroup(
    val family: String,
    val openings: List<OpeningStats>,
    val isExpanded: Boolean = false
) {
    val totalPlayed: Int get() = openings.sumOf { it.played }
    val totalWins: Int get() = openings.sumOf { it.wins }
    val winPct: Float get() = if (totalPlayed == 0) 0f else totalWins.toFloat() / totalPlayed
}

fun OpeningStatsRaw.toOpeningStats(name: String, family: String) = OpeningStats(
    ecoCode = ecoCode,
    openingName = name,
    family = family,
    played = played,
    wins = wins,
    losses = losses,
    draws = draws,
    gamesInBook = gamesInBook,
    gamesDeviated = gamesDeviated
)
