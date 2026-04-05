package com.example.chessrepertoiretrainer.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Implementation of [GameFetcher] for the public Chess.com archives API.
 */
object ChessComGameFetcher : GameFetcher {

    private const val BASE_URL = "https://api.chess.com/pub/player"

    override val platformKey: String = "chess.com"

    override suspend fun fetchGamesForUser(
        username: String,
        since: Long?,
        maxGames: Int?
    ): List<FetchedGame> = withContext(Dispatchers.IO) {
        val normalizedUser = username.trim().lowercase()
        if (normalizedUser.isBlank()) return@withContext emptyList()

        // 1. Get archive list for the user.
        val archivesUrl = "$BASE_URL/$normalizedUser/games/archives"
        val archivesBody = httpGet(archivesUrl) ?: return@withContext emptyList()

        val archivesJson = JSONObject(archivesBody)
        val archivesArray = archivesJson.optJSONArray("archives") ?: return@withContext emptyList()
        if (archivesArray.length() == 0) return@withContext emptyList()

        val archiveUrls = mutableListOf<String>()
        for (i in 0 until archivesArray.length()) {
            val value = archivesArray.optString(i, null)
            if (!value.isNullOrBlank()) {
                archiveUrls += value
            }
        }
        if (archiveUrls.isEmpty()) return@withContext emptyList()

        val effectiveMaxGames = maxGames?.coerceAtLeast(1) ?: Int.MAX_VALUE
        val result = mutableListOf<FetchedGame>()

        // Process archives from newest to oldest.
        outer@ for (archiveUrl in archiveUrls.asReversed()) {
            val archiveBody = httpGet(archiveUrl) ?: continue
            val archiveJson = JSONObject(archiveBody)
            val gamesArray = archiveJson.optJSONArray("games") ?: continue

            for (i in 0 until gamesArray.length()) {
                if (result.size >= effectiveMaxGames) break@outer

                val gameJson = gamesArray.optJSONObject(i) ?: continue
                val pgn = gameJson.optString("pgn", "")
                if (pgn.isBlank()) continue

                val headers = parsePgnHeaders(pgn)
                val resultTag = headers["Result"] ?: "*"
                val timeControlTag = headers["TimeControl"]

                val endTimeSec = gameJson.optLong("end_time", 0L)
                val playedAt = if (endTimeSec > 0L) endTimeSec * 1000L else 0L

                if (since != null && since > 0L && playedAt > 0L && playedAt <= since) {
                    // Game is older than our last sync threshold.
                    continue
                }

                val whiteObj = gameJson.optJSONObject("white")
                val blackObj = gameJson.optJSONObject("black")
                val whiteUser = whiteObj?.optString("username") ?: "White"
                val blackUser = blackObj?.optString("username") ?: "Black"

                val isUserWhite = normalizedUser == whiteUser.trim().lowercase()
                val isUserBlack = normalizedUser == blackUser.trim().lowercase()

                if (!isUserWhite && !isUserBlack) {
                    // Not this player's game (should be rare).
                    continue
                }

                val opponentName = if (isUserWhite) blackUser else whiteUser
                val rated = gameJson.optBoolean("rated", false)

                val timeControlFromJsonRaw = gameJson.optString("time_control", "")
                val timeControlFromJson = timeControlFromJsonRaw.takeIf { it.isNotBlank() }
                val timeControl = timeControlTag ?: timeControlFromJson

                // Use Chess.com "time_class" field to derive a normalized
                // time‑control category instead of inferring it heuristically
                // from the numeric time.
                val timeClassRaw = gameJson.optString("time_class", "")
                val timeCategory = mapChessComTimeClassToCategory(timeClassRaw)

                val uuidRaw = gameJson.optString("uuid", "")
                val urlRaw = gameJson.optString("url", "")
                val uuid = uuidRaw.takeIf { it.isNotBlank() }
                val url = urlRaw.takeIf { it.isNotBlank() }
                val platformGameId = when {
                    !uuid.isNullOrBlank() -> uuid
                    !url.isNullOrBlank() -> url
                    else -> "${normalizedUser}_${playedAt}_${resultTag}"
                }

                result += FetchedGame(
                    platformGameId = platformGameId,
                    opponentName = opponentName,
                    isUserWhite = isUserWhite,
                    result = resultTag,
                    timeControl = timeControl,
                    timeCategory = timeCategory,
                    rated = rated,
                    playedAt = playedAt,
                    pgn = pgn
                )
            }
        }

        Log.d(
            "ChessComGameFetcher",
            "Fetched ${result.size} games for $normalizedUser (since=${since ?: "null"}, max=$maxGames)"
        )

        return@withContext result
    }

    private fun mapChessComTimeClassToCategory(timeClass: String?): String? {
        val normalized = timeClass?.trim()?.lowercase().orEmpty()
        return when (normalized) {
            "bullet" -> "bullet"
            "blitz" -> "blitz"
            "rapid" -> "rapid"
            // Chess.com uses "daily" for correspondence/daily chess.
            "daily" -> "classical"
            else -> null
        }
    }

    private fun httpGet(urlString: String): String? {
        return try {
            Log.d("ChessComGameFetcher", "HTTP GET $urlString")
            val url = URL(urlString)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15_000
                readTimeout = 30_000
            }

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                Log.w("ChessComGameFetcher", "HTTP error $responseCode for $urlString")
                return null
            }

            connection.inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            Log.e("ChessComGameFetcher", "HTTP GET error for $urlString: ${e.message}", e)
            null
        }
    }
}


