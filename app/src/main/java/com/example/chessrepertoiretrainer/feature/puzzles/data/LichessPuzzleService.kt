package com.example.chessrepertoiretrainer.feature.puzzles.data

import android.util.Log
import com.example.chessrepertoiretrainer.core.database.entity.Puzzle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object LichessPuzzleService {

    /**
     * Lichess daily puzzle endpoint returns JSON objects of the form:
     *
     * {
     *   "game": { ... },
     *   "puzzle": {
     *     "id": "XXXXXX",
     *     "rating": 1600,
     *     "solution": ["e2e4", "e7e5", ...],   // UCI moves
     *     "themes": ["crushing", "middlegame", ...],
     *     ...
     *   }
     * }
     *
     * We store:
     *   - [com.example.chessrepertoiretrainer.core.database.entity.Puzzle.fen]: FEN string when available (may come from puzzle, game, or the root object).
     *   - [com.example.chessrepertoiretrainer.core.database.entity.Puzzle.moves]: the `puzzle.solution` array joined into a single space‑separated string
     *     (these are UCI moves as provided by Lichess).
     */

    // Daily endpoint used for fetching a single puzzle.
    private const val DAILY_PUZZLE_URL = "https://lichess.org/api/puzzle/daily"

    /** Fetch a single daily puzzle. */
    suspend fun fetchDailyPuzzle(): Puzzle? = withContext(Dispatchers.IO) {
        try {
            val startTime = System.currentTimeMillis()
            Log.d("LichessPuzzleService", "Requesting daily puzzle from $DAILY_PUZZLE_URL")

            val url = URL(DAILY_PUZZLE_URL)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 10_000
            }

            val responseCode = connection.responseCode
            Log.d(
                "LichessPuzzleService",
                "Daily puzzle HTTP response code=$responseCode in ${System.currentTimeMillis() - startTime}ms"
            )
            if (responseCode != HttpURLConnection.HTTP_OK) {
                Log.e("LichessPuzzleService", "HTTP error: $responseCode")
                return@withContext null
            }

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            Log.d(
                "LichessPuzzleService",
                "Daily puzzle raw body length=${body.length}, preview='${body.take(200)}'"
            )

            val root = JSONObject(body)
            val puzzleJson = root.getJSONObject("puzzle")
            parsePuzzleFromJson(root, puzzleJson, sourceTag = "daily")
        }
        catch (e: Exception) {
            Log.e("LichessPuzzleService", "Error fetching daily puzzle: ${e.message}", e)
            null
        }
    }


    /** Common JSON → Puzzle mapping used by the daily endpoint. */
    private fun parsePuzzleFromJson(
        root: JSONObject, puzzleJson: JSONObject, sourceTag: String
    ): Puzzle? {
        val lichessId = puzzleJson.optString("id", "")
        if (lichessId.isBlank()) {
            Log.w("LichessPuzzleService", "[$sourceTag] Missing puzzle id, skipping")
            return null
        }

        val localId = "${lichessId}_${System.currentTimeMillis()}"
        val rating = puzzleJson.optInt("rating", 1500)

        // FEN może być w obiekcie puzzle, game lub w korzeniu odpowiedzi – obsłuż wszystkie.
        val fenFromPuzzle = puzzleJson.optString("fen", "")
        val fenFromRoot = root.optString("fen", "")
        val fenFromGame = root.optJSONObject("game")?.optString("fen", "") ?: ""
        val fen =
            listOf(fenFromPuzzle, fenFromRoot, fenFromGame).firstOrNull { it.isNotBlank() } ?: ""
        Log.d(
            "LichessPuzzleService",
            "[$sourceTag] Puzzle $lichessId rating=$rating fenFromPuzzle='${fenFromPuzzle.take(32)}' fenFromRoot='${
                fenFromRoot.take(
                    32
                )
            }' fenFromGame='${fenFromGame.take(32)}' chosenFen='${fen.take(32)}'"
        )

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
        Log.d(
            "LichessPuzzleService",
            "[$sourceTag] Puzzle $lichessId solution tokensCount=${tokens.size} looksLikeUci=$looksLikeUci firstTokens=${
                tokens.take(
                    4
                )
            } themes='$themes'"
        )

        if (fen.isBlank() || moves.isBlank()) {
            Log.w(
                "LichessPuzzleService",
                "[$sourceTag] Skipping puzzle $lichessId due to missing fen or moves (fen='${
                    fen.take(
                        16
                    )
                }', moves='$moves')"
            )
            return null
        }

        Log.d(
            "LichessPuzzleService",
            "[$sourceTag] Creating local puzzle id=$localId rating=$rating fen='${fen.take(32)}' movesTokens=${tokens.size}"
        )

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