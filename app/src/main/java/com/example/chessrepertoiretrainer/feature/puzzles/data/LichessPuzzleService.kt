package com.example.chessrepertoiretrainer.feature.puzzles.data

import android.util.Log
import com.example.chessrepertoiretrainer.core.database.entity.Puzzle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object LichessPuzzleService {

    private const val DAILY_PUZZLE_URL = "https://lichess.org/api/puzzle/daily"

    suspend fun fetchDailyPuzzle(): Puzzle? = withContext(Dispatchers.IO) {
        try {
            val startTime = System.currentTimeMillis()

            val url = URL(DAILY_PUZZLE_URL)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 10_000
            }

            val responseCode = connection.responseCode

            if (responseCode != HttpURLConnection.HTTP_OK) {
                Log.e("LichessPuzzleService", "HTTP error: $responseCode")
                return@withContext null
            }

            val body = connection.inputStream.bufferedReader().use { it.readText() }

            val root = JSONObject(body)
            val puzzleJson = root.getJSONObject("puzzle")
            parsePuzzleFromJson(root, puzzleJson, sourceTag = "daily")
        }
        catch (e: Exception) {
            Log.e("LichessPuzzleService", "Error fetching daily puzzle: ${e.message}", e)
            null
        }
    }


    private fun parsePuzzleFromJson(
        root: JSONObject, puzzleJson: JSONObject, sourceTag: String
    ): Puzzle? {
        val lichessId = puzzleJson.optString("id", "")
        if (lichessId.isBlank()) {
            return null
        }

        val localId = "${lichessId}_${System.currentTimeMillis()}"
        val rating = puzzleJson.optInt("rating", 1500)

        val fenFromPuzzle = puzzleJson.optString("fen", "")
        val fenFromRoot = root.optString("fen", "")
        val fenFromGame = root.optJSONObject("game")?.optString("fen", "") ?: ""
        val fen =
            listOf(fenFromPuzzle, fenFromRoot, fenFromGame).firstOrNull { it.isNotBlank() } ?: ""

        val solutionArray = puzzleJson.optJSONArray("solution")
        val moves = if (solutionArray != null && solutionArray.length() > 0) {
            (0 until solutionArray.length()).joinToString(" ") { idx ->
                solutionArray.getString(idx)
            }
        }
        else {
            ""
        }

        val themesArray = puzzleJson.optJSONArray("themes")
        val themes = if (themesArray != null && themesArray.length() > 0) {
            (0 until themesArray.length()).joinToString(",") { idx ->
                themesArray.getString(idx)
            }
        }
        else {
            ""
        }

        val tokens = moves.split(" ").filter { it.isNotBlank() }
        val looksLikeUci = tokens.isNotEmpty() && tokens.all { token ->
            token.length in 4..5 && token[0] in 'a'..'h' && token[1] in '1'..'8' && token[2] in 'a'..'h' && token[3] in '1'..'8'
        }

        if (fen.isBlank() || moves.isBlank()) {
            return null
        }

        return Puzzle(
            id = localId,
            fen = fen,
            moves = moves,
            rating = rating,
            themes = themes,
            isSolved = false,
            attempts = 0
        )
    }
}