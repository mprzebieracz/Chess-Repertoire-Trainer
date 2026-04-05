package com.example.chessrepertoiretrainer.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Implementation of [GameFetcher] for Lichess user games API.
 *
 * It uses the NDJSON endpoint with `pgnInJson=true` so that we can access
 * both metadata and full PGN for each game.
 */
object LichessGameFetcher : GameFetcher {

    private const val BASE_URL = "https://lichess.org/api/games/user"

    override val platformKey: String = "lichess"

    override suspend fun fetchGamesForUser(
        username: String,
        since: Long?,
        maxGames: Int?
    ): List<FetchedGame> = withContext(Dispatchers.IO) {
        val normalizedUser = username.trim()
        if (normalizedUser.isBlank()) return@withContext emptyList()

        val params = mutableListOf(
            "pgnInJson=true",
            "clocks=false",
            "evals=false",
            "accuracy=false",
            "opening=true"
        )
        if (maxGames != null && maxGames > 0) {
            params += "max=$maxGames"
        }
        if (since != null && since > 0L) {
            params += "since=$since"
        }

        val urlString = "$BASE_URL/$normalizedUser?${params.joinToString("&")}"
        Log.d("LichessGameFetcher", "Requesting games from $urlString")

        val url = URL(urlString)
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 30_000
            setRequestProperty("Accept", "application/x-ndjson")
        }

        val responseCode = connection.responseCode
        if (responseCode != HttpURLConnection.HTTP_OK) {
            Log.w(
                "LichessGameFetcher",
                "HTTP error $responseCode when fetching games for $normalizedUser"
            )
            return@withContext emptyList()
        }

        val result = mutableListOf<FetchedGame>()

        connection.inputStream.bufferedReader().useLines { lines ->
            lines.forEach { line ->
                if (line.isBlank()) return@forEach

                try {
                    val obj = JSONObject(line)
                    val pgn = obj.optString("pgn", "")
                    if (pgn.isBlank()) return@forEach

                    val headers = parsePgnHeaders(pgn)
                    val resultTag = headers["Result"] ?: "*"
                    val timeControlTag = headers["TimeControl"]

                    val playersObj = obj.optJSONObject("players")
                    val whiteObj = playersObj?.optJSONObject("white")
                    val blackObj = playersObj?.optJSONObject("black")

                    val whiteUser =
                        whiteObj?.optJSONObject("user")?.optString("name")
                            ?: whiteObj?.optString("userId")
                            ?: "White"
                    val blackUser =
                        blackObj?.optJSONObject("user")?.optString("name")
                            ?: blackObj?.optString("userId")
                            ?: "Black"

                    val isUserWhite =
                        normalizedUser.equals(whiteUser, ignoreCase = true)
                    val isUserBlack =
                        normalizedUser.equals(blackUser, ignoreCase = true)

                    if (!isUserWhite && !isUserBlack) {
                        // This should not normally happen, but be defensive.
                        return@forEach
                    }

                    val opponentName = if (isUserWhite) blackUser else whiteUser
                    val rated = obj.optBoolean("rated", false)

                    val clockObj = obj.optJSONObject("clock")
                    val tcFromClock = clockObj?.let {
                        val initial = it.optLong("initial", 0L)
                        val increment = it.optLong("increment", 0L)
                        if (initial > 0L) "$initial+$increment" else null
                    }
                    val timeControl = timeControlTag ?: tcFromClock

                    val createdAt = obj.optLong("createdAt", 0L)
                    val lastMoveAt = obj.optLong("lastMoveAt", 0L)
                    val playedAt = if (lastMoveAt > 0L) lastMoveAt else createdAt

                    val platformGameId = obj.optString("id").takeIf { it.isNotBlank() }
                        ?: "${normalizedUser}_${playedAt}_${resultTag}"

                    result += FetchedGame(
                        platformGameId = platformGameId,
                        opponentName = opponentName,
                        isUserWhite = isUserWhite,
                        result = resultTag,
                        timeControl = timeControl,
                        rated = rated,
                        playedAt = playedAt,
                        pgn = pgn
                    )
                } catch (e: Exception) {
                    Log.e("LichessGameFetcher", "Error parsing game line: ${e.message}", e)
                }
            }
        }

        Log.d(
            "LichessGameFetcher",
            "Fetched ${result.size} games for $normalizedUser (since=${since ?: "null"}, max=$maxGames)"
        )

        return@withContext result
    }
}

