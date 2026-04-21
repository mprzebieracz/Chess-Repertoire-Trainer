package com.example.chessrepertoiretrainer.feature.openingtree.data.fetcher

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object ChessComGameFetcher : GameFetcher {

    private const val BASE_URL = "https://api.chess.com/pub/player"
    override val platformKey: String = "chess.com"

    override suspend fun fetchGamesForUser(
        username: String, since: Long?, maxGames: Int?
    ): List<FetchedGame> = withContext(Dispatchers.IO) {
        val normalizedUser = username.trim().lowercase()
        if (normalizedUser.isBlank()) return@withContext emptyList()

        val archiveUrls = fetchArchiveUrls(normalizedUser)
        if (archiveUrls.isEmpty()) return@withContext emptyList()

        val effectiveMaxGames = maxGames?.coerceAtLeast(1) ?: Int.MAX_VALUE
        val result = mutableListOf<FetchedGame>()

        // Process archives from newest to oldest
        for (archiveUrl in archiveUrls.asReversed()) {
            if (result.size >= effectiveMaxGames) break

            val gamesInArchive = fetchGamesFromArchive(archiveUrl, normalizedUser, since)
            for (game in gamesInArchive) {
                if (result.size >= effectiveMaxGames) break
                result.add(game)
            }
        }

        Log.d(
            "ChessComGameFetcher",
            "Fetched ${result.size} games for $normalizedUser (since=$since, max=$maxGames)"
        )
        return@withContext result
    }

    private fun fetchArchiveUrls(username: String): List<String> {
        val body = httpGet("$BASE_URL/$username/games/archives") ?: return emptyList()
        val array = JSONObject(body).optJSONArray("archives") ?: return emptyList()

        return List(array.length()) { array.optString(it) }.filter { it.isNotBlank() }
    }

    private fun fetchGamesFromArchive(
        archiveUrl: String,
        username: String,
        since: Long?
    ): List<FetchedGame> {
        val body = httpGet(archiveUrl) ?: return emptyList()
        val gamesArray = JSONObject(body).optJSONArray("games") ?: return emptyList()

        return List(gamesArray.length()) { gamesArray.optJSONObject(it) }.mapNotNull {
                parseChessComGame(
                    it,
                    username,
                    since
                )
            }
    }

    private fun parseChessComGame(
        gameJson: JSONObject?,
        username: String,
        since: Long?
    ): FetchedGame? {
        if (gameJson == null) return null
        val pgn = gameJson.optString("pgn", "")
        if (pgn.isBlank()) return null

        val playedAt = gameJson.optLong("end_time", 0L) * 1000L
        if (since != null && since > 0L && playedAt in 1..since) {
            return null // Skip games older than our sync threshold
        }

        val whiteUser = gameJson.optJSONObject("white")?.optString("username", "") ?: ""
        val blackUser = gameJson.optJSONObject("black")?.optString("username", "") ?: ""

        val isUserWhite = username.equals(whiteUser.trim(), ignoreCase = true)
        val isUserBlack = username.equals(blackUser.trim(), ignoreCase = true)
        if (!isUserWhite && !isUserBlack) return null

        val headers = parsePgnHeaders(pgn)
        val resultTag = headers["Result"] ?: "*"

        val timeControlFromJson = gameJson.optString("time_control", "").takeIf { it.isNotBlank() }
        val timeControl = headers["TimeControl"] ?: timeControlFromJson

        val uuid = gameJson.optString("uuid", "").takeIf { it.isNotBlank() }
        val url = gameJson.optString("url", "").takeIf { it.isNotBlank() }

        return FetchedGame(
            platformGameId = uuid ?: url ?: "${username}_${playedAt}_${resultTag}",
            opponentName = if (isUserWhite) blackUser else whiteUser,
            isUserWhite = isUserWhite,
            result = resultTag,
            timeControl = timeControl,
            timeCategory = mapTimeClassToCategory(gameJson.optString("time_class", "")),
            rated = gameJson.optBoolean("rated", false),
            playedAt = playedAt,
            pgn = pgn
        )
    }

    private fun mapTimeClassToCategory(timeClass: String?): String? {
        return when (timeClass?.trim()?.lowercase()) {
            "bullet" -> "bullet"
            "blitz" -> "blitz"
            "rapid" -> "rapid"
            "daily" -> "classical" // Chess.com correspondence
            else -> null
        }
    }

    private fun httpGet(urlString: String): String? {
        return runCatching {
            val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15_000
                readTimeout = 30_000
            }
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.bufferedReader().use { it.readText() }
            }
            else {
                Log.w("ChessComGameFetcher", "HTTP error ${connection.responseCode} for $urlString")
                null
            }
        }.onFailure {
            Log.e("ChessComGameFetcher", "HTTP GET error for $urlString: ${it.message}")
        }.getOrNull()
    }
}