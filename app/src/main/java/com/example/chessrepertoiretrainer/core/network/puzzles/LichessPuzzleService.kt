package com.example.chessrepertoiretrainer.core.network.puzzles

import android.util.Log
import com.example.chessrepertoiretrainer.core.database.entity.Puzzle
import com.example.chessrepertoiretrainer.core.network.openGetConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection

object LichessPuzzleService {

    private const val DAILY_PUZZLE_URL = "https://lichess.org/api/puzzle/daily"

    suspend fun fetchDailyPuzzle(sourceDate: String): Puzzle? = withContext(Dispatchers.IO) {
        try {
            val connection = openGetConnection(DAILY_PUZZLE_URL, 10_000, 10_000)

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                Log.e("LichessPuzzleService", "HTTP error: ${connection.responseCode}")
                return@withContext null
            }

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            parsePuzzle(JSONObject(body), sourceDate)
        } catch (e: Exception) {
            Log.e("LichessPuzzleService", "Error fetching daily puzzle: ${e.message}", e)
            null
        }
    }

    private fun parsePuzzle(root: JSONObject, sourceDate: String): Puzzle? {
        val puzzleJson = root.getJSONObject("puzzle")

        val lichessId = puzzleJson.optString("id", "")
        if (lichessId.isBlank()) return null

        val fen = listOf(
            puzzleJson.optString("fen", ""),
            root.optString("fen", ""),
            root.optJSONObject("game")?.optString("fen", "")
                ?: ""
        ).firstOrNull { it.isNotBlank() } ?: return null

        val solutionArray = puzzleJson.optJSONArray("solution") ?: return null
        if (solutionArray.length() == 0) return null
        val moves =
            (0 until solutionArray.length()).joinToString(" ") { solutionArray.getString(it) }

        val themesArray = puzzleJson.optJSONArray("themes")
        val themes = if (themesArray != null) {
            (0 until themesArray.length()).joinToString(",") { themesArray.getString(it) }
        } else {
            ""
        }

        return Puzzle(
            id = "${lichessId}_$sourceDate",
            fen = fen,
            moves = moves,
            rating = puzzleJson.optInt("rating", 1500),
            themes = themes,
            isSolved = false,
            attempts = 0,
            sourceDate = sourceDate
        )
    }
}