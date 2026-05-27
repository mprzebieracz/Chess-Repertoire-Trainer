package com.example.chessrepertoiretrainer.core.network.explorer

import android.util.Log
import com.example.chessrepertoiretrainer.core.database.dao.LichessExplorerCacheDao
import com.example.chessrepertoiretrainer.core.database.entity.LichessExplorerCache
import com.example.chessrepertoiretrainer.core.network.openGetConnection
import com.example.chessrepertoiretrainer.feature.settings.data.UserSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder

private const val TAG = "LichessExplorerService"
private const val CONNECT_TIMEOUT = 10_000
private const val READ_TIMEOUT = 60_000
private const val TTL_LICHESS_MS = 24 * 60 * 60 * 1000L
private const val TTL_MASTERS_MS = 30 * 24 * 60 * 60 * 1000L
private const val AUTH_SENTINEL = "AUTH_REQUIRED_401"

class LichessExplorerService(
    private val cacheDao: LichessExplorerCacheDao,
    private val settingsRepository: UserSettingsRepository? = null
) {

    suspend fun getPlayerStats(
        fen: String,
        speeds: List<String> = listOf("blitz", "rapid"),
        ratings: List<Int> = listOf(1600, 1800, 2000)
    ): ExplorerResponse? = withContext(Dispatchers.IO) {
        val key = cacheKey("lichess", fen, speeds, ratings)
        val cached = cacheDao.getByKey(key)
        if (cached != null && !isExpired(cached.fetchedAt, TTL_LICHESS_MS) && cached.jsonPayload != AUTH_SENTINEL) {
            Log.d(TAG, "Cache hit for players FEN")
            val parsed = parseResponse(cached.jsonPayload)
            if (parsed != null) return@withContext parsed
            Log.w(TAG, "Cached payload failed to parse — re-fetching")
        }

        val encodedFen = URLEncoder.encode(fen, "UTF-8").replace("+", "%20")
        val speedsParam = speeds.joinToString(",")
        val ratingsParam = ratings.joinToString(",")
        val url = "https://explorer.lichess.org/lichess?fen=$encodedFen&speeds=$speedsParam&ratings=$ratingsParam&moves=10&recentGames=0"

        val json = fetchRaw(url) ?: return@withContext null
        if (json == AUTH_SENTINEL) {
            return@withContext ExplorerResponse(0, 0, 0, emptyList(), null, emptyList(), requiresAuth = true)
        }
        Log.d(TAG, "Players response length: ${json.length}")
        cacheDao.upsert(LichessExplorerCache(key, fen, "lichess", speedsParam, ratingsParam, json, System.currentTimeMillis()))
        parseResponse(json)
    }

    suspend fun getMastersStats(fen: String): ExplorerResponse? = withContext(Dispatchers.IO) {
        val key = cacheKey("masters", fen, emptyList(), emptyList())
        val cached = cacheDao.getByKey(key)
        if (cached != null && !isExpired(cached.fetchedAt, TTL_MASTERS_MS) && cached.jsonPayload != AUTH_SENTINEL) {
            Log.d(TAG, "Cache hit for masters FEN")
            val parsed = parseResponse(cached.jsonPayload)
            if (parsed != null) return@withContext parsed
            Log.w(TAG, "Cached payload failed to parse — re-fetching")
        }

        val encodedFen = URLEncoder.encode(fen, "UTF-8").replace("+", "%20")
        val url = "https://explorer.lichess.org/masters?fen=$encodedFen&moves=10&topGames=15"

        val json = fetchRaw(url) ?: return@withContext null
        if (json == AUTH_SENTINEL) {
            return@withContext ExplorerResponse(0, 0, 0, emptyList(), null, emptyList(), requiresAuth = true)
        }
        Log.d(TAG, "Masters response length: ${json.length}")
        cacheDao.upsert(LichessExplorerCache(key, fen, "masters", "", "", json, System.currentTimeMillis()))
        parseResponse(json)
    }

    suspend fun getMasterGamePgn(gameId: String): String? =
        withContext(Dispatchers.IO) {
            val result = fetchRaw("https://explorer.lichess.org/masters/pgn/$gameId", acceptTextPlain = true)
            if (result == AUTH_SENTINEL) null else result
        }

    private suspend fun fetchRaw(urlString: String, acceptTextPlain: Boolean = false): String? {
        val token = settingsRepository?.settingsFlow?.first()?.lichessApiToken?.trim() ?: ""
        return try {
            val conn = openGetConnection(
                urlString, CONNECT_TIMEOUT, READ_TIMEOUT,
                if (acceptTextPlain) "text/plain" else "application/json"
            )
            if (token.isNotEmpty()) conn.setRequestProperty("Authorization", "Bearer $token")
            val code = conn.responseCode
            when {
                code == 401 -> {
                    Log.w(TAG, "HTTP 401 for $urlString — add a Lichess API token in Settings")
                    AUTH_SENTINEL
                }
                code != HttpURLConnection.HTTP_OK -> {
                    Log.w(TAG, "HTTP $code for $urlString")
                    null
                }
                else -> conn.inputStream.bufferedReader().readText()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fetch failed for $urlString: ${e.message}")
            null
        }
    }

    private fun parseResponse(json: String): ExplorerResponse? {
        return try {
            val obj = JSONObject(json)

            val movesArray = obj.optJSONArray("moves")
            val moves = mutableListOf<ExplorerMove>()
            if (movesArray != null) {
                for (i in 0 until movesArray.length()) {
                    val m = movesArray.getJSONObject(i)
                    moves.add(
                        ExplorerMove(
                            uci = m.optString("uci"),
                            san = m.optString("san"),
                            white = m.optLong("white"),
                            draws = m.optLong("draws"),
                            black = m.optLong("black")
                        )
                    )
                }
            }
            Log.d(TAG, "Parsed ${moves.size} moves")

            val openingObj = obj.optJSONObject("opening")
            val opening = openingObj?.let {
                ExplorerOpening(
                    it.optString("eco").ifEmpty { null },
                    it.optString("name").ifEmpty { null }
                )
            }

            val topGamesArray = obj.optJSONArray("topGames")
            val topGames = mutableListOf<MasterGameEntry>()
            if (topGamesArray != null) {
                for (i in 0 until topGamesArray.length()) {
                    val g = topGamesArray.getJSONObject(i)
                    topGames.add(
                        MasterGameEntry(
                            id = g.optString("id"),
                            white = g.optJSONObject("white")?.optString("name") ?: "?",
                            black = g.optJSONObject("black")?.optString("name") ?: "?",
                            year = g.optInt("year"),
                            winner = g.optString("winner").ifEmpty { null }
                        )
                    )
                }
            }

            ExplorerResponse(
                white = obj.optLong("white"),
                draws = obj.optLong("draws"),
                black = obj.optLong("black"),
                moves = moves,
                opening = opening,
                topGames = topGames
            )
        } catch (e: Exception) {
            Log.e(TAG, "Parse failed: ${e.message}")
            null
        }
    }

    private fun cacheKey(db: String, fen: String, speeds: List<String>, ratings: List<Int>): String =
        "${db}_${fen}_${speeds.sorted().joinToString()}_${ratings.sorted().joinToString()}"

    private fun isExpired(fetchedAt: Long, ttl: Long): Boolean =
        System.currentTimeMillis() - fetchedAt > ttl
}
