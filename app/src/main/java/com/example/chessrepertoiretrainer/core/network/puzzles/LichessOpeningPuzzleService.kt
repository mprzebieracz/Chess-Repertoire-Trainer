package com.example.chessrepertoiretrainer.core.network.puzzles

import android.util.Log
import com.example.chessrepertoiretrainer.core.chess.domain.findLegalMoveBySan
import com.example.chessrepertoiretrainer.core.chess.pgn.extract.PGNExtractor
import com.example.chessrepertoiretrainer.core.database.entity.Puzzle
import com.example.chessrepertoiretrainer.core.network.openGetConnection
import com.github.bhlangonijr.chesslib.Board
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection

private const val TAG = "LichessOpeningPuzzleService"
private const val CONNECT_TIMEOUT = 10_000
private const val READ_TIMEOUT = 15_000

object LichessOpeningPuzzleService {

    suspend fun getPuzzlesByOpening(openingFamily: String, count: Int): List<Puzzle> =
        withContext(Dispatchers.IO) {
            val result = mutableListOf<Puzzle>()
            val today = java.time.LocalDate.now().toString()
            repeat(count) {
                val puzzle = fetchNext(openingFamily, today) ?: return@repeat
                if (result.none { it.id == puzzle.id }) result.add(puzzle)
            }
            result
        }

    internal fun familyToAngle(family: String): String =
        family.trim()
            .lowercase()
            .replace("'", "")
            .replace(Regex("[^a-z0-9 -]"), "")
            .replace("-", "_")
            .replace(" ", "_")

    private fun fetchNext(openingFamily: String, sourceDate: String): Puzzle? {
        return try {
            val angle = familyToAngle(openingFamily)
            val url = "https://lichess.org/api/puzzle/next?angle=$angle"
            val conn = openGetConnection(url, CONNECT_TIMEOUT, READ_TIMEOUT)
            if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                Log.w(TAG, "HTTP ${conn.responseCode} fetching opening puzzle for $openingFamily")
                return null
            }
            val json = conn.inputStream.bufferedReader().readText()
            parsePuzzle(json, openingFamily, sourceDate)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch opening puzzle for $openingFamily", e)
            null
        }
    }

    internal fun parsePuzzle(json: String, openingFamily: String, sourceDate: String): Puzzle? {
        return try {
            val root = JSONObject(json)
            val puzzleObj = root.getJSONObject("puzzle")
            val gameObj = root.optJSONObject("game")

            val id = puzzleObj.getString("id")
            val initialPly = puzzleObj.optInt("initialPly", 0)
            val pgn = gameObj?.optString("pgn", "") ?: ""

            // initialPly is the 0-indexed position of the trigger move in the PGN array;
            // the puzzle position is AFTER that move, so play initialPly+1 moves.
            val fen = fenAtPly(pgn, initialPly + 1) ?: run {
                Log.w(TAG, "Could not derive FEN for puzzle $id (ply=$initialPly)")
                return null
            }

            val solutionArray = puzzleObj.getJSONArray("solution")
            val moves = (0 until solutionArray.length())
                .joinToString(" ") { solutionArray.getString(it) }
            val rating = puzzleObj.optInt("rating", 1500)
            val themesArray = puzzleObj.optJSONArray("themes")
            val themes = if (themesArray != null) {
                (0 until themesArray.length()).joinToString(",") { themesArray.getString(it) }
            } else ""

            Puzzle(
                id = id,
                fen = fen,
                moves = moves,
                rating = rating,
                themes = themes,
                isSolved = false,
                attempts = 0,
                sourceDate = sourceDate,
                openingFamily = openingFamily,
                source = "opening",
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse opening puzzle", e)
            null
        }
    }

    internal fun fenAtPly(pgn: String, ply: Int): String? {
        return try {
            val board = Board()
            val sanMoves = PGNExtractor.extractSanMovesFromPgn(pgn)
            for (san in sanMoves.take(ply)) {
                board.findLegalMoveBySan(san)?.let { board.doMove(it) } ?: return null
            }
            board.fen
        } catch (e: Exception) {
            Log.e(TAG, "Failed to derive FEN from PGN at ply $ply", e)
            null
        }
    }
}
