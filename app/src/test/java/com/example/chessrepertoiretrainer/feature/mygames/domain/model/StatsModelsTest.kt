package com.example.chessrepertoiretrainer.feature.mygames.domain.model

import org.junit.Assert.*
import org.junit.Test

class StatsModelsTest {

    // ---- GameStats ----

    @Test
    fun `GameStats winPct is wins over played`() {
        val stats = GameStats(played = 10, wins = 3, losses = 5, draws = 2)
        assertEquals(0.3f, stats.winPct, 0.001f)
    }

    @Test
    fun `GameStats lossPct is losses over played`() {
        val stats = GameStats(played = 10, wins = 3, losses = 5, draws = 2)
        assertEquals(0.5f, stats.lossPct, 0.001f)
    }

    @Test
    fun `GameStats drawPct is draws over played`() {
        val stats = GameStats(played = 10, wins = 3, losses = 5, draws = 2)
        assertEquals(0.2f, stats.drawPct, 0.001f)
    }

    @Test
    fun `GameStats zero played returns zero for all pcts`() {
        val stats = GameStats(played = 0, wins = 0, losses = 0, draws = 0)
        assertEquals(0f, stats.winPct, 0f)
        assertEquals(0f, stats.lossPct, 0f)
        assertEquals(0f, stats.drawPct, 0f)
    }

    // ---- OpeningStats ----

    @Test
    fun `OpeningStats winPct is wins over played`() {
        val stats = opening(played = 4, wins = 1)
        assertEquals(0.25f, stats.winPct, 0.001f)
    }

    @Test
    fun `OpeningStats inBookPct is gamesInBook over played`() {
        val stats = opening(played = 10, gamesInBook = 7)
        assertEquals(0.7f, stats.inBookPct, 0.001f)
    }

    @Test
    fun `OpeningStats zero played returns zero pcts`() {
        val stats = opening(played = 0)
        assertEquals(0f, stats.winPct, 0f)
        assertEquals(0f, stats.inBookPct, 0f)
    }

    // ---- OpeningFamilyGroup ----

    @Test
    fun `OpeningFamilyGroup totalPlayed sums openings`() {
        val group = OpeningFamilyGroup(
            family = "Sicilian",
            openings = listOf(opening(played = 3), opening(played = 5))
        )
        assertEquals(8, group.totalPlayed)
    }

    @Test
    fun `OpeningFamilyGroup totalWins sums openings`() {
        val group = OpeningFamilyGroup(
            family = "Sicilian",
            openings = listOf(opening(wins = 2), opening(wins = 3))
        )
        assertEquals(5, group.totalWins)
    }

    @Test
    fun `OpeningFamilyGroup winPct is totalWins over totalPlayed`() {
        val group = OpeningFamilyGroup(
            family = "Sicilian",
            openings = listOf(opening(played = 4, wins = 2), opening(played = 6, wins = 3))
        )
        assertEquals(0.5f, group.winPct, 0.001f)
    }

    @Test
    fun `OpeningFamilyGroup winPct zero played returns 0`() {
        val group = OpeningFamilyGroup(family = "X", openings = emptyList())
        assertEquals(0f, group.winPct, 0f)
    }

    // ---- ChapterStats ----

    @Test
    fun `ChapterStats winPct is wins over played`() {
        val stats = ChapterStats(played = 8, wins = 4, losses = 3, draws = 1, gamesInBook = 0, gamesDeviated = 0, avgBookDepthPlies = null)
        assertEquals(0.5f, stats.winPct, 0.001f)
    }

    @Test
    fun `ChapterStats inBookPct is gamesInBook over played`() {
        val stats = ChapterStats(played = 10, wins = 0, losses = 0, draws = 0, gamesInBook = 6, gamesDeviated = 0, avgBookDepthPlies = null)
        assertEquals(0.6f, stats.inBookPct, 0.001f)
    }

    @Test
    fun `ChapterStats winPctInBook uses overall winPct as approximation`() {
        val stats = ChapterStats(played = 8, wins = 4, losses = 3, draws = 1, gamesInBook = 5, gamesDeviated = 0, avgBookDepthPlies = null)
        assertEquals(stats.winPct, stats.winPctInBook, 0.001f)
    }

    @Test
    fun `ChapterStats winPctInBook returns 0 when gamesInBook is 0`() {
        val stats = ChapterStats(played = 8, wins = 4, losses = 3, draws = 1, gamesInBook = 0, gamesDeviated = 0, avgBookDepthPlies = null)
        assertEquals(0f, stats.winPctInBook, 0f)
    }

    @Test
    fun `ChapterStats zero played returns zero pcts`() {
        val stats = ChapterStats(played = 0, wins = 0, losses = 0, draws = 0, gamesInBook = 0, gamesDeviated = 0, avgBookDepthPlies = null)
        assertEquals(0f, stats.winPct, 0f)
        assertEquals(0f, stats.inBookPct, 0f)
    }

    // ---- StatsTimeRange ----

    @Test
    fun `ALL_TIME sinceEpochMs returns 0`() {
        assertEquals(0L, StatsTimeRange.ALL_TIME.sinceEpochMs())
    }

    @Test
    fun `DAYS_7 sinceEpochMs is approximately now minus 7 days`() {
        val expected = System.currentTimeMillis() - 7L * 86_400_000L
        val actual = StatsTimeRange.DAYS_7.sinceEpochMs()
        assertTrue(kotlin.math.abs(actual - expected) < 2_000L)
    }

    @Test
    fun `DAYS_30 sinceEpochMs is earlier than DAYS_7`() {
        assertTrue(StatsTimeRange.DAYS_30.sinceEpochMs() < StatsTimeRange.DAYS_7.sinceEpochMs())
    }

    // ---- helpers ----

    private fun opening(
        played: Int = 10,
        wins: Int = 0,
        losses: Int = 0,
        draws: Int = 0,
        gamesInBook: Int = 0
    ) = OpeningStats(
        ecoCode = "A00",
        openingName = "Test",
        family = "Test",
        played = played,
        wins = wins,
        losses = losses,
        draws = draws,
        gamesInBook = gamesInBook,
        gamesDeviated = 0
    )
}
