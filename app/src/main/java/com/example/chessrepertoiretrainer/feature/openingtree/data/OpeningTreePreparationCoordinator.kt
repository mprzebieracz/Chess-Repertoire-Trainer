package com.example.chessrepertoiretrainer.feature.openingtree.data

import com.example.chessrepertoiretrainer.feature.openingtree.data.fetcher.FetchedGame
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
            color = color.trim().lowercase().ifEmpty { "both" },
            timeControlFilter = timeControlFilter.trim(),
            maxGamesForTree = maxGamesForTree
        )
    }

    suspend fun prepareFromFetchedGames(
        username: String,
        platform: String,
        games: List<FetchedGame>, // These are ALREADY filtered by the Fetchers!
        color: String,
        timeControlFilter: String,
        maxGamesForTree: Int?
    ): Int {
        if (games.isEmpty()) {
            OpeningTreeCache.clearForUser(username, platform)
            return 0
        }

        val tree = withContext(Dispatchers.Default) {
            OpeningTreeBuilder.buildTree(games.map {
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
                    color = color,
                    timeControlFilter = timeControlFilter,
                    maxGamesForTree = maxGamesForTree
                ), tree
            )
            return games.size
        }

        return 0
    }
}