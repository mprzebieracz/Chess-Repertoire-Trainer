package com.example.chessrepertoiretrainer.core.network.games

import com.example.chessrepertoiretrainer.core.network.openGetConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.time.Instant
import java.time.ZoneOffset

object ChessComGameFetcher : GameFetcher {

    private const val BASE_URL = "https://api.chess.com/pub/player"
    override val platformKey: String = "chess.com"

    override suspend fun fetchGamesForUser(
        username: String,
        maxGames: Int?,
        colorFilter: String,
        timeControlFilter: String,
        ratedOnly: Boolean,
        since: Long?,
        onProgress: ((fetched: Int) -> Unit)?
    ): List<FetchedGame> =
        withContext(Dispatchers.IO) {
            val normalizedUser = username.trim().lowercase()
            if (normalizedUser.isBlank()) return@withContext emptyList()

            val archiveUrls = fetchArchiveUrls(normalizedUser)
            if (archiveUrls.isEmpty()) return@withContext emptyList()

            val effectiveMaxGames = maxGames?.coerceAtLeast(1) ?: Int.MAX_VALUE
            val timeControls = timeControlFilter.split(",").map { it.trim().lowercase() }
                .filter { it.isNotBlank() }.toSet()

            val sinceYearMonth = since?.let { epochMsToYearMonth(it) }
            val result = mutableListOf<FetchedGame>()

            for (archiveUrl in archiveUrls.asReversed()) {
                if (result.size >= effectiveMaxGames) break
                if (sinceYearMonth != null && archiveUrlYearMonth(archiveUrl) < sinceYearMonth) break

                val gamesInArchive = fetchGamesFromArchive(archiveUrl, normalizedUser)
                for (game in gamesInArchive) {
                    if (result.size >= effectiveMaxGames) break
                    if (since != null && game.playedAt <= since) continue

                    if (ratedOnly && !game.rated) continue
                    if (colorFilter == "white" && !game.isUserWhite) continue
                    if (colorFilter == "black" && game.isUserWhite) continue

                    if (timeControls.isNotEmpty()) {
                        val category = game.timeCategory ?: continue
                        if (category !in timeControls) continue
                    }

                    result.add(game)
                    onProgress?.invoke(result.size)
                }
            }

            return@withContext result
        }

    internal fun epochMsToYearMonth(epochMs: Long): Int {
        val date = Instant.ofEpochMilli(epochMs).atOffset(ZoneOffset.UTC)
        return date.year * 100 + date.monthValue
    }

    internal fun archiveUrlYearMonth(url: String): Int {
        val parts = url.trimEnd('/').split("/")
        val month = parts.getOrNull(parts.size - 1)?.toIntOrNull() ?: return 0
        val year = parts.getOrNull(parts.size - 2)?.toIntOrNull() ?: return 0
        return year * 100 + month
    }

    private fun fetchArchiveUrls(username: String): List<String> {
        val body = httpGet("$BASE_URL/$username/games/archives") ?: return emptyList()
        val array = JSONObject(body).optJSONArray("archives") ?: return emptyList()
        return List(array.length()) { array.optString(it) }.filter { it.isNotBlank() }
    }

    private fun fetchGamesFromArchive(archiveUrl: String, username: String): List<FetchedGame> {
        val body = httpGet(archiveUrl) ?: return emptyList()
        val gamesArray = JSONObject(body).optJSONArray("games") ?: return emptyList()
        return List(gamesArray.length()) { gamesArray.optJSONObject(it) }.mapNotNull {
            parseChessComGame(it, username)
        }
    }

    internal fun parseChessComGame(gameJson: JSONObject?, username: String): FetchedGame? {
        if (gameJson == null) return null
        val pgn = gameJson.optString("pgn", "")
        if (pgn.isBlank()) return null

        val playedAt = gameJson.optLong("end_time", 0L) * 1000L

        val whiteUser = gameJson.optJSONObject("white")?.optString("username", "") ?: ""
        val blackUser = gameJson.optJSONObject("black")?.optString("username", "") ?: ""

        val isUserWhite = username.equals(whiteUser.trim(), ignoreCase = true)
        val isUserBlack = username.equals(blackUser.trim(), ignoreCase = true)
        if (!isUserWhite && !isUserBlack) return null

        val headers = parsePgnHeaders(pgn)
        val resultTag = headers["Result"] ?: "*"
        val timeControlFromJson = gameJson.optString("time_control", "").takeIf { it.isNotBlank() }
        val uuid = gameJson.optString("uuid", "").takeIf { it.isNotBlank() }
        val url = gameJson.optString("url", "").takeIf { it.isNotBlank() }
        val opening = (headers["Opening"] ?: headers["ECO"])?.takeIf { it.isNotBlank() }
        val whiteRating = gameJson.optJSONObject("white")?.optInt("rating", -1)?.takeIf { it > 0 }
        val blackRating = gameJson.optJSONObject("black")?.optInt("rating", -1)?.takeIf { it > 0 }

        return FetchedGame(
            platformGameId = uuid ?: url ?: "${username}_${playedAt}_${resultTag}",
            opponentName = if (isUserWhite) blackUser else whiteUser,
            isUserWhite = isUserWhite,
            result = resultTag,
            timeControl = headers["TimeControl"] ?: timeControlFromJson,
            timeCategory = mapTimeClassToCategory(
                gameJson.optString(
                    "time_class",
                    ""
                )
            ),
            opening = opening,
            playerRating = if (isUserWhite) whiteRating else blackRating,
            opponentRating = if (isUserWhite) blackRating else whiteRating,
            rated = gameJson.optBoolean("rated", false),
            playedAt = playedAt,
            pgn = pgn
        )
    }

    internal fun mapTimeClassToCategory(timeClass: String?): String? {
        return when (timeClass?.trim()?.lowercase()) {
            "bullet" -> "bullet"
            "blitz" -> "blitz"
            "rapid" -> "rapid"
            "daily" -> "classical"
            else -> null
        }
    }

    private fun httpGet(urlString: String): String? {
        val connection = openGetConnection(urlString, 15_000, 30_000)
        return if (connection.responseCode == HttpURLConnection.HTTP_OK) {
            connection.inputStream.bufferedReader().use { it.readText() }
        } else {
            null
        }
    }
}