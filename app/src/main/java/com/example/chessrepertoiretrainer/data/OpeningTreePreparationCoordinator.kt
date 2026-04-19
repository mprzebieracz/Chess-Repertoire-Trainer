package com.example.chessrepertoiretrainer.data

import com.example.chessrepertoiretrainer.ui.viewmodels.OpeningTreeFilterUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Shared coordinator for preparing and caching opening trees.
 *
 * Centralizes normalize -> filter -> limit -> build -> cache flow so viewmodels
 * stay focused on UI orchestration and status updates.
 */
class OpeningTreePreparationCoordinator {

    fun buildCacheKey(
        profileId: Long,
        color: String,
        timeControlFilter: String,
        maxGamesForTree: Int?
    ): OpeningTreeCacheKey {
        val normalizedColor = normalizeColor(color)
        val normalizedTimeControl = timeControlFilter.trim()
        return OpeningTreeCacheKey(
            profileId = profileId,
            color = normalizedColor,
            timeControlFilter = normalizedTimeControl,
            maxGamesForTree = maxGamesForTree
        )
    }

    suspend fun prepareFromStoredGames(
        profileId: Long,
        games: List<GameWithPgn>,
        color: String,
        timeControlFilter: String,
        maxGamesForTree: Int?
    ): Int {
        if (games.isEmpty()) {
            OpeningTreeCache.clearForProfile(profileId)
            return 0
        }

        val normalizedColor = normalizeColor(color)
        val normalizedTimeControl = timeControlFilter.trim()

        val filtered = OpeningTreeFilterUtils.filterGamesByColor(
            games = games,
            color = normalizedColor,
            timeControlFilter = normalizedTimeControl
        )
        if (filtered.isEmpty()) {
            OpeningTreeCache.clearForProfile(profileId)
            return 0
        }

        val limited = maxGamesForTree?.let { filtered.take(it) } ?: filtered
        if (limited.isEmpty()) {
            OpeningTreeCache.clearForProfile(profileId)
            return 0
        }

        val tree = withContext(Dispatchers.Default) {
            val gamesForTree = limited.map { gwp ->
                GameForOpeningTree(
                    pgn = gwp.pgn,
                    isUserWhite = gwp.game.isUserWhite,
                    resultTag = gwp.game.result
                )
            }
            OpeningTreeBuilder.buildTree(gamesForTree)
        }

        OpeningTreeCache.clearForProfile(profileId)

        if (tree != null) {
            val key = OpeningTreeCacheKey(
                profileId = profileId,
                color = normalizedColor,
                timeControlFilter = normalizedTimeControl,
                maxGamesForTree = maxGamesForTree
            )
            OpeningTreeCache.put(key, tree)
            return limited.size
        }

        return 0
    }

    suspend fun prepareFromFetchedGames(
        profileId: Long,
        games: List<FetchedGame>,
        color: String,
        timeControlFilter: String,
        maxGamesForTree: Int?
    ): Int {
        if (games.isEmpty()) {
            OpeningTreeCache.clearForProfile(profileId)
            return 0
        }

        val normalizedColor = normalizeColor(color)
        val normalizedTimeControl = timeControlFilter.trim()

        val filtered = OpeningTreeFilterUtils.filterFetchedGames(
            games = games,
            color = normalizedColor,
            timeControlFilter = normalizedTimeControl
        )
        if (filtered.isEmpty()) {
            OpeningTreeCache.clearForProfile(profileId)
            return 0
        }

        val limited = maxGamesForTree?.let { filtered.take(it) } ?: filtered
        if (limited.isEmpty()) {
            OpeningTreeCache.clearForProfile(profileId)
            return 0
        }

        val tree = withContext(Dispatchers.Default) {
            val gamesForTree = limited.map { fetched ->
                GameForOpeningTree(
                    pgn = fetched.pgn,
                    isUserWhite = fetched.isUserWhite,
                    resultTag = fetched.result
                )
            }
            OpeningTreeBuilder.buildTree(gamesForTree)
        }

        OpeningTreeCache.clearForProfile(profileId)

        if (tree != null) {
            val key = OpeningTreeCacheKey(
                profileId = profileId,
                color = normalizedColor,
                timeControlFilter = normalizedTimeControl,
                maxGamesForTree = maxGamesForTree
            )
            OpeningTreeCache.put(key, tree)
            return limited.size
        }

        return 0
    }

    private fun normalizeColor(color: String): String {
        return color.trim().lowercase().ifEmpty { "both" }
    }
}

