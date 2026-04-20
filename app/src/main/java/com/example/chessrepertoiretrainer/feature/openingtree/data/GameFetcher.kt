package com.example.chessrepertoiretrainer.feature.openingtree.data

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

/**
 * Abstraction for platform-specific game download implementations.
 */
interface GameFetcher {

    /** Platform key that should match [PlayerProfile.platform], e.g. "lichess". */
    val platformKey: String

    /**
     * Fetch games for a given user.
     *
     * @param username Platform username (case-insensitive).
     * @param since Only games strictly newer than this timestamp (milliseconds since epoch)
     * should be returned when supported by the underlying API.
     * @param maxGames Optional upper bound on number of games to return.
     */
    suspend fun fetchGamesForUser(
        username: String, since: Long? = null, maxGames: Int? = null
    ): List<FetchedGame>
}

/**
 * Simple registry mapping a platform string to its corresponding [GameFetcher].
 */
class GameFetcherRegistry(
    fetchers: List<GameFetcher>
) {
    private val byPlatformKey: Map<String, GameFetcher> = fetchers.associateBy { it.platformKey.lowercase() }

    fun getFetcher(platform: String): GameFetcher? = byPlatformKey[platform.lowercase()]
}

/**
 * Helper for extracting standard PGN headers from a raw PGN string. We keep it here so
 * both Lichess and Chess.com fetchers can use it.
 */
internal fun parsePgnHeaders(pgn: String): Map<String, String> {
    val headers = mutableMapOf<String, String>()
    for (rawLine in pgn.lineSequence()) {
        val line = rawLine.trim()
        if (line.isEmpty()) continue

        if (line.startsWith("[") && line.endsWith("]")) {
            val match = TAG_REGEX.matchEntire(line)
            if (match != null) {
                val key = match.groupValues[1]
                val value = match.groupValues[2]
                headers[key] = value
            }
        }
        else {
            // First non-header, non-empty line – headers section is finished.
            break
        }
    }

    if (headers.isEmpty()) {
        Log.d("GameFetcher", "parsePgnHeaders: no headers detected in PGN, length=${pgn.length}")
    }

    return headers
}

private val TAG_REGEX = Regex("""\[(\w+)\s+"(.*)"]""")

