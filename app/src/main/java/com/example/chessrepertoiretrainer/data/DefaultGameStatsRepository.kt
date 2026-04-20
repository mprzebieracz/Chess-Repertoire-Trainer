package com.example.chessrepertoiretrainer.data

import com.example.chessrepertoiretrainer.database.dao.GameStatsDao
import com.example.chessrepertoiretrainer.database.entities.GameStats
import com.example.chessrepertoiretrainer.domain.games.GamesRepository
import com.example.chessrepertoiretrainer.domain.stats.GameStatsRepository
import com.example.chessrepertoiretrainer.domain.stats.GameStatsSummary
import com.example.chessrepertoiretrainer.domain.stats.OpeningStatsRow

/**
 * Default implementation of [GameStatsRepository] backed by the existing
 * [GamesRepository] and the [GameStatsDao]. It computes per-game statistics
 * from PGNs using the same PGN parser as the opening-tree builder, then
 * exposes lightweight aggregated summaries for UI consumption.
 */
class DefaultGameStatsRepository(
    private val gamesRepository: GamesRepository, private val gameStatsDao: GameStatsDao
) : GameStatsRepository {

    override suspend fun recomputeStatsForProfile(profileId: Long) {
        val gamesWithPgn = gamesRepository.getGamesWithPgnForProfile(profileId)
        if (gamesWithPgn.isEmpty()) {
            gameStatsDao.deleteForProfile(profileId)
            return
        }

        val stats = gamesWithPgn.mapNotNull { gwp ->
            val sanMoves = OpeningTreeBuilder.extractSanMovesFromPgn(gwp.pgn)
            if (sanMoves.isEmpty()) return@mapNotNull null

            val openingKey = buildOpeningKey(sanMoves)
            val result = resultFromUserPerspective(gwp.game.result, gwp.game.isUserWhite) ?: return@mapNotNull null

            val plyCount = sanMoves.size
            val userMoveCount = countUserMoves(plyCount, gwp.game.isUserWhite)
            val opponentMoveCount = plyCount - userMoveCount

            GameStats(
                gameId = gwp.game.id,
                profileId = gwp.game.profileId,
                openingKey = openingKey,
                color = if (gwp.game.isUserWhite) "white" else "black",
                timeCategory = gwp.game.timeCategory ?: "unknown",
                result = result,
                isRated = gwp.game.rated,
                plyCount = plyCount,
                userMoveCount = userMoveCount,
                opponentMoveCount = opponentMoveCount
            )
        }

        if (stats.isEmpty()) {
            gameStatsDao.deleteForProfile(profileId)
        }
        else {
            gameStatsDao.deleteForProfile(profileId)
            gameStatsDao.upsertAll(stats)
        }
    }

    override suspend fun loadSummaryForProfile(profileId: Long): GameStatsSummary? {
        val stats = gameStatsDao.getStatsForProfile(profileId)
        if (stats.isEmpty()) return null

        var wins = 0
        var draws = 0
        var losses = 0
        val byTimeCategory = mutableMapOf<String, Int>()
        val byOpening = mutableMapOf<String, MutableList<Int>>()

        stats.forEach { s ->
            when (s.result) {
                "win" -> wins++
                "draw" -> draws++
                "loss" -> losses++
            }

            byTimeCategory[s.timeCategory] = (byTimeCategory[s.timeCategory] ?: 0) + 1

            val score = when (s.result) {
                "win" -> 100
                "draw" -> 50
                else -> 0
            }
            byOpening.getOrPut(s.openingKey) { mutableListOf() }.add(score)
        }

        val totalGames = wins + draws + losses
        if (totalGames == 0) return null

        val winPercent = (wins * 100) / totalGames

        val topOpenings = byOpening.entries.map { (key, scores) ->
                val games = scores.size
                val avgScore = scores.sum() / games
                OpeningStatsRow(
                    openingKey = key, games = games, scorePercent = avgScore
                )
            }.sortedWith(compareByDescending<OpeningStatsRow> { it.games }.thenByDescending { it.scorePercent }).take(5)

        return GameStatsSummary(
            totalGames = totalGames,
            wins = wins,
            draws = draws,
            losses = losses,
            winPercent = winPercent,
            byTimeCategory = byTimeCategory,
            topOpenings = topOpenings
        )
    }

    /**
     * Build a short "opening key" from the first couple of moves in SAN.
     *
     * We intentionally cap this at the first two plies (e.g. "e4 e5", "e4 c5")
     * so that the UI can show compact, easy-to-scan labels without needing a
     * full ECO classifier or an opening book. If there are fewer than two
     * moves, we just join whatever is available.
     */
    private fun buildOpeningKey(sanMoves: List<String>): String {
        if (sanMoves.isEmpty()) return "(no moves)"
        return sanMoves.take(2).joinToString(" ")
    }

    private fun countUserMoves(plyCount: Int, isUserWhite: Boolean): Int {
        // White moves at even plies (0, 2, 4, ...), Black at odd plies.
        return if (isUserWhite) {
            (plyCount + 1) / 2
        }
        else {
            plyCount / 2
        }
    }

    /**
     * Map the raw PGN result tag (e.g. "1-0") and user color to a simple
     * win/draw/loss label for this game. Returns null for unsupported tags.
     */
    private fun resultFromUserPerspective(resultTag: String, isUserWhite: Boolean): String? {
        return when (resultTag) {
            "1-0" -> if (isUserWhite) "win" else "loss"
            "0-1" -> if (isUserWhite) "loss" else "win"
            "1/2-1/2" -> "draw"
            else -> null
        }
    }
}



