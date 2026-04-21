package com.example.chessrepertoiretrainer.feature.openingtree.data

import com.example.chessrepertoiretrainer.feature.openingtree.presentation.OpeningTreeFilterUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OpeningTreePreparationCoordinator {

    fun buildCacheKey(
        username: String,
        platform: String,
        color: String,
        timeControlFilter: String,
        maxGamesForTree: Int?
    ): OpeningTreeCacheKey {
        return OpeningTreeCacheKey(
            username = username.trim(),
            platform = platform.trim().lowercase(),
            color = normalizeColor(color),
            timeControlFilter = timeControlFilter.trim(),
            maxGamesForTree = maxGamesForTree
        )
    }

    suspend fun prepareFromFetchedGames(
        username: String,
        platform: String,
        games: List<FetchedGame>,
        color: String,
        timeControlFilter: String,
        maxGamesForTree: Int?
    ): Int {
        if (games.isEmpty()) {
            OpeningTreeCache.clearForUser(username, platform)
            return 0
        }

        val normalizedColor = normalizeColor(color)
        val normalizedTimeControl = timeControlFilter.trim()
        val filtered =
            OpeningTreeFilterUtils.filterFetchedGames(games, normalizedColor, normalizedTimeControl)

        if (filtered.isEmpty()) {
            OpeningTreeCache.clearForUser(username, platform)
            return 0
        }

        val limited = maxGamesForTree?.let { filtered.take(it) } ?: filtered
        if (limited.isEmpty()) {
            OpeningTreeCache.clearForUser(username, platform)
            return 0
        }

        val tree = withContext(Dispatchers.Default) {
            OpeningTreeBuilder.buildTree(limited.map {
                GameForOpeningTree(
                    pgn = it.pgn, isUserWhite = it.isUserWhite, resultTag = it.result
                )
            })
        }

        OpeningTreeCache.clearForUser(username, platform)
        if (tree != null) {
            OpeningTreeCache.put(
                buildCacheKey(
                    username = username,
                    platform = platform,
                    color = normalizedColor,
                    timeControlFilter = normalizedTimeControl,
                    maxGamesForTree = maxGamesForTree
                ), tree
            )
            return limited.size
        }

        return 0
    }

    private fun normalizeColor(color: String): String {
        return color.trim().lowercase().ifEmpty { "both" }
    }
}