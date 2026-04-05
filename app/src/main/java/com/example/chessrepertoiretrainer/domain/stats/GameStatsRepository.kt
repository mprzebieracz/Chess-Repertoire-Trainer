package com.example.chessrepertoiretrainer.domain.stats

/**
 * High-level summary of a player's results across their games.
 */
data class OpeningStatsRow(
    val openingKey: String,
    val games: Int,
    val scorePercent: Int
)

/**
 * Aggregated statistics for a profile, derived from per-game [GameStats]
 * rows. This is optimized for display in the UI.
 */
data class GameStatsSummary(
    val totalGames: Int,
    val wins: Int,
    val draws: Int,
    val losses: Int,
    val winPercent: Int,
    val byTimeCategory: Map<String, Int>,
    val topOpenings: List<OpeningStatsRow>
)

interface GameStatsRepository {

    /**
     * Recompute per-game statistics for all games belonging to the given
     * profile. This parses PGNs once and stores derived data in the
     * [game_stats] table so that subsequent aggregations are fast.
     */
    suspend fun recomputeStatsForProfile(profileId: Long)

    /**
     * Load an aggregated summary for the given profile from the stored
     * [game_stats] rows. Returns null if there are no games.
     */
    suspend fun loadSummaryForProfile(profileId: Long): GameStatsSummary?
}

