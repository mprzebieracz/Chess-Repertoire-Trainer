package com.example.chessrepertoiretrainer.feature.openingtree.data.fetcher

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object LichessGameFetcher : GameFetcher {

    private const val BASE_URL = "https://lichess.org/api/games/user"
    override val platformKey: String = "lichess"

    override suspend fun fetchGamesForUser(username: String,
                                           maxGames: Int?,
                                           colorFilter: String,
                                           timeControlFilter: String,
                                           since: Long?,
                                           onProgress: ((fetched: Int) -> Unit)?): List<FetchedGame> =
        withContext(Dispatchers.IO) {
            val normalizedUser = username.trim()
            if (normalizedUser.isBlank()) return@withContext emptyList()

            val urlString =
                buildRequestUrl(normalizedUser, maxGames, colorFilter, timeControlFilter, since)
            return@withContext streamGames(urlString, normalizedUser, onProgress)
        }

    private fun buildRequestUrl(username: String,
                                maxGames: Int?,
                                colorFilter: String,
                                timeControlFilter: String,
                                since: Long?): String {
        val params = mutableListOf("pgnInJson=true",
                                   "clocks=false",
                                   "evals=false",
                                   "accuracy=false",
                                   "opening=true")
        if (maxGames != null && maxGames > 0) params += "max=$maxGames"
        if (since != null) params += "since=$since"

        if (colorFilter == "white" || colorFilter == "black") {
            params += "color=$colorFilter"
        }

        if (timeControlFilter.isNotBlank()) {
            val perfTypes = timeControlFilter.split(",").map { it.trim().lowercase() }
                .filter { it.isNotBlank() }.joinToString(",")
            if (perfTypes.isNotBlank()) {
                params += "perfType=$perfTypes"
            }
        }

        return "$BASE_URL/$username?${params.joinToString("&")}"
    }

    private fun streamGames(urlString: String,
                            username: String,
                            onProgress: ((Int) -> Unit)?): List<FetchedGame> {
        val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 30_000
            setRequestProperty("Accept", "application/x-ndjson")
        }

        if (connection.responseCode != HttpURLConnection.HTTP_OK) {
            Log.w("LichessGameFetcher", "HTTP error ${connection.responseCode} for $username")
            return emptyList()
        }

        var fetched = 0
        return connection.inputStream.bufferedReader().useLines { lines ->
            lines.mapNotNull { line ->
                parseLichessGame(line, username)?.also {
                    fetched++
                    onProgress?.invoke(fetched)
                }
            }.toList()
        }
    }

    private fun parseLichessGame(jsonLine: String, username: String): FetchedGame? {
        if (jsonLine.isBlank()) return null

        return runCatching {
            val obj = JSONObject(jsonLine)
            val pgn = obj.optString("pgn", "")
            if (pgn.isBlank()) return null

            val players = obj.optJSONObject("players")
            val whiteUser = extractUsername(players?.optJSONObject("white"), "White")
            val blackUser = extractUsername(players?.optJSONObject("black"), "Black")

            val isUserWhite = username.equals(whiteUser, ignoreCase = true)
            val isUserBlack = username.equals(blackUser, ignoreCase = true)
            if (!isUserWhite && !isUserBlack) return null

            val headers = parsePgnHeaders(pgn)
            val resultTag = headers["Result"] ?: "*"
            val playedAt =
                obj.optLong("lastMoveAt", 0L).takeIf { it > 0L } ?: obj.optLong("createdAt", 0L)

            val clockObj = obj.optJSONObject("clock")
            val tcFromClock = clockObj?.let {
                val initial = it.optLong("initial", 0L)
                val increment = it.optLong("increment", 0L)
                if (initial > 0L) "$initial+$increment" else null
            }

            val gameId = obj.optString("id").takeIf { it.isNotBlank() }
                ?: "${username}_${playedAt}_${resultTag}"

            val opening =
                obj.optJSONObject("opening")?.optString("name")?.takeIf { it.isNotBlank() }
            val whiteRating =
                players?.optJSONObject("white")?.optInt("rating", -1)?.takeIf { it > 0 }
            val blackRating =
                players?.optJSONObject("black")?.optInt("rating", -1)?.takeIf { it > 0 }

            FetchedGame(platformGameId = gameId,
                        opponentName = if (isUserWhite) blackUser else whiteUser,
                        isUserWhite = isUserWhite,
                        result = resultTag,
                        timeControl = headers["TimeControl"] ?: tcFromClock,
                        timeCategory = mapSpeedToCategory(obj.optString("speed", "")),
                        opening = opening,
                        playerRating = if (isUserWhite) whiteRating else blackRating,
                        opponentRating = if (isUserWhite) blackRating else whiteRating,
                        rated = obj.optBoolean("rated", false),
                        playedAt = playedAt,
                        pgn = pgn)
        }.getOrNull()
    }

    private fun extractUsername(playerObj: JSONObject?, fallback: String): String {
        return playerObj?.optJSONObject("user")?.optString("name") ?: playerObj?.optString("userId")
        ?: fallback
    }

    private fun mapSpeedToCategory(speed: String?): String? {
        return when (speed?.trim()?.lowercase()) {
            "ultrabullet", "bullet" -> "bullet"
            "blitz" -> "blitz"
            "rapid" -> "rapid"
            "classical", "correspondence" -> "classical"
            else -> null
        }
    }
}