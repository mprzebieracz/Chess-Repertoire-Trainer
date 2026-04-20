package com.example.chessrepertoiretrainer.feature.openingtree.data

import com.example.chessrepertoiretrainer.feature.openingtree.presentation.OpeningTreeFilterUtils
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
        profileId: Long, color: String, timeControlFilter: String, maxGamesForTree: Int?
    ): OpeningTreeCacheKey {
        return OpeningTreeCacheKey(
            profileId = profileId,
            color = normalizeColor(color),
            timeControlFilter = timeControlFilter.trim(),
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
        return prepareAndCache(
            profileId = profileId,
            games = games,
            color = color,
            timeControlFilter = timeControlFilter,
            maxGamesForTree = maxGamesForTree,
            filter = OpeningTreeFilterUtils::filterGamesByColor,
            toTreeGame = { gwp ->
                GameForOpeningTree(
                    pgn = gwp.pgn,
                    isUserWhite = gwp.game.isUserWhite,
                    resultTag = gwp.game.result
                )
            }
        )
    }

    suspend fun prepareFromFetchedGames(
        profileId: Long,
        games: List<FetchedGame>,
        color: String,
        timeControlFilter: String,
        maxGamesForTree: Int?
    ): Int {
        return prepareAndCache(
            profileId = profileId,
            games = games,
            color = color,
            timeControlFilter = timeControlFilter,
            maxGamesForTree = maxGamesForTree,
            filter = OpeningTreeFilterUtils::filterFetchedGames,
            toTreeGame = { fetched ->
                GameForOpeningTree(
                    pgn = fetched.pgn,
                    isUserWhite = fetched.isUserWhite,
                    resultTag = fetched.result
                )
            }
        )
    }

    private fun normalizeColor(color: String): String {
        return color.trim().lowercase().ifEmpty { "both" }
    }

    private suspend fun <T> prepareAndCache(
        profileId: Long,
        games: List<T>,
        color: String,
        timeControlFilter: String,
        maxGamesForTree: Int?,
        filter: (List<T>, String, String) -> List<T>,
        toTreeGame: (T) -> GameForOpeningTree
    ): Int {
        if (games.isEmpty()) {
            OpeningTreeCache.clearForProfile(profileId)
            return 0
        }

        val normalizedColor = normalizeColor(color)
        val normalizedTimeControl = timeControlFilter.trim()
        val filtered = filter(games, normalizedColor, normalizedTimeControl)
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
            OpeningTreeBuilder.buildTree(limited.map(toTreeGame))
        }

        OpeningTreeCache.clearForProfile(profileId)
        if (tree != null) {
            OpeningTreeCache.put(
                buildCacheKey(
                    profileId = profileId,
                    color = normalizedColor,
                    timeControlFilter = normalizedTimeControl,
                    maxGamesForTree = maxGamesForTree
                ),
                tree
            )
            return limited.size
        }

        return 0
    }
}

