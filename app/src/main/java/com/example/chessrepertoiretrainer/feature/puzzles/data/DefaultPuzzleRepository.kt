package com.example.chessrepertoiretrainer.feature.puzzles.data

import android.util.Log
import com.example.chessrepertoiretrainer.core.database.dao.PuzzleDao
import com.example.chessrepertoiretrainer.core.database.entity.Puzzle
import com.example.chessrepertoiretrainer.feature.puzzles.PuzzleRepository
import kotlinx.coroutines.flow.Flow

/**
 * Default implementation of [PuzzleRepository] that uses Room for local
 * storage and [LichessPuzzleService] for fetching new puzzles.
 */
class DefaultPuzzleRepository(
    private val puzzleDao: PuzzleDao
) : PuzzleRepository {

    override fun getAllPuzzles(): Flow<List<Puzzle>> = puzzleDao.getAllPuzzles()

    override suspend fun getUnsolvedCount(): Int = puzzleDao.countUnsolvedPuzzles()

    override suspend fun ensureMinUnsolvedPuzzles(minUnsolved: Int): Int {
        val current = puzzleDao.countUnsolvedPuzzles()
        if (current > 0) {
            Log.d(
                "DefaultPuzzleRepository", "ensureMinUnsolvedPuzzles: already have $current unsolved puzzles (daily mode)"
            )
            return current
        }

        // Daily mode: if there are no unsolved puzzles, try to download the
        // current daily puzzle from Lichess and insert it.
        Log.d(
            "DefaultPuzzleRepository", "ensureMinUnsolvedPuzzles: no unsolved puzzles, fetching daily puzzle from Lichess"
        )

        val daily = LichessPuzzleService.fetchDailyPuzzle()
        if (daily == null) {
            Log.w(
                "DefaultPuzzleRepository", "ensureMinUnsolvedPuzzles: failed to fetch daily puzzle from Lichess"
            )
            return 0
        }

        return try {
            Log.d(
                "DefaultPuzzleRepository",
                "Inserting daily puzzle id=${daily.id} rating=${daily.rating} " + "fen='${daily.fen.take(32)}' movesTokens=${
                    daily.moves.split(" ").count { it.isNotBlank() }
                }")
            puzzleDao.insertPuzzle(daily)

            val finalCount = puzzleDao.countUnsolvedPuzzles()
            Log.d(
                "DefaultPuzzleRepository", "ensureMinUnsolvedPuzzles: unsolved after=$finalCount (daily mode, requestedMin=$minUnsolved)"
            )
            finalCount
        }
        catch (e: Exception) {
            Log.e(
                "DefaultPuzzleRepository", "Failed to insert daily puzzle id=${daily.id}: ${e.message}", e
            )
            puzzleDao.countUnsolvedPuzzles()
        }
    }

    override suspend fun getRandomUnsolvedPuzzle(): Puzzle? = puzzleDao.getRandomUnsolvedPuzzle()

    override suspend fun updatePuzzleStats(id: String, isSolved: Boolean, attempts: Int) {
        puzzleDao.updatePuzzleStats(id = id, isSolved = isSolved, attempts = attempts)
    }
}