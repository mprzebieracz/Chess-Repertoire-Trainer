package com.example.chessrepertoiretrainer.feature.openingtree.data.fetcher

import android.util.Log

data class FetchedGame(
    val platformGameId: String,
    val opponentName: String,
    val isUserWhite: Boolean,
    val result: String,
    val timeControl: String?,
    val timeCategory: String?,
    val rated: Boolean,
    val playedAt: Long,
    val pgn: String
)

interface GameFetcher {
    val platformKey: String

    suspend fun fetchGamesForUser(
        username: String, since: Long? = null, maxGames: Int? = null
    ): List<FetchedGame>
}

class GameFetcherRegistry(fetchers: List<GameFetcher>) {
    private val byPlatformKey: Map<String, GameFetcher> =
        fetchers.associateBy { it.platformKey.lowercase() }

    fun getFetcher(platform: String): GameFetcher? = byPlatformKey[platform.trim().lowercase()]
}

private val TAG_REGEX = Regex("""\[(\w+)\s+"(.*)"]""")

internal fun parsePgnHeaders(pgn: String): Map<String, String> {
    val headers = mutableMapOf<String, String>()

    for (line in pgn.lineSequence().map { it.trim() }) {
        if (line.isEmpty()) continue

        val match = TAG_REGEX.matchEntire(line)
        if (match != null) {
            headers[match.groupValues[1]] = match.groupValues[2]
        }
        else if (!line.startsWith("[")) {
            break
        }
    }

    if (headers.isEmpty()) {
        Log.d("GameFetcher", "No headers detected in PGN (length=${pgn.length})")
    }

    return headers
}