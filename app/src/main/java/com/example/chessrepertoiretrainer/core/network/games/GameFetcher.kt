package com.example.chessrepertoiretrainer.core.network.games

data class FetchedGame(
    val platformGameId: String,
    val opponentName: String,
    val isUserWhite: Boolean,
    val result: String,
    val timeControl: String?,
    val timeCategory: String?,
    val opening: String?,
    val playerRating: Int?,
    val opponentRating: Int?,
    val rated: Boolean,
    val playedAt: Long,
    val pgn: String
)

interface GameFetcher {
    val platformKey: String

    suspend fun fetchGamesForUser(
        username: String,
        maxGames: Int? = null,
        colorFilter: String = "both",
        timeControlFilter: String = "",
        ratedOnly: Boolean = false,
        since: Long? = null,
        onProgress: ((fetched: Int) -> Unit)? = null
    ): List<FetchedGame>

    // Streams games to `onBatch` in fixed-size chunks rather than accumulating everything
    // in memory first. Used by GameSyncManager; defaults to a single full-list batch so
    // fetchers that don't override still work correctly.
    suspend fun streamGamesForUser(
        username: String,
        since: Long? = null,
        onProgress: ((fetched: Int) -> Unit)? = null,
        onBatch: suspend (List<FetchedGame>) -> Unit,
    ) {
        val all = fetchGamesForUser(username = username, since = since, onProgress = onProgress)
        if (all.isNotEmpty()) onBatch(all)
    }
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

    return headers
}