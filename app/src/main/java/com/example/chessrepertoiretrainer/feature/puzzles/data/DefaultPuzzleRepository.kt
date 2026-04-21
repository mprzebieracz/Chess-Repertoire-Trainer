package com.example.chessrepertoiretrainer.feature.puzzles.data

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
            return current
        }

        val daily = LichessPuzzleService.fetchDailyPuzzle()
        if (daily == null) {
            return 0
        }

        return try {
            puzzleDao.insertPuzzle(daily)

            val finalCount = puzzleDao.countUnsolvedPuzzles()
            finalCount
        }
        catch (e: Exception) {
            puzzleDao.countUnsolvedPuzzles()
        }
    }

    override suspend fun getRandomUnsolvedPuzzle(): Puzzle? = puzzleDao.getRandomUnsolvedPuzzle()

    override suspend fun updatePuzzleStats(id: String, isSolved: Boolean, attempts: Int) {
        puzzleDao.updatePuzzleStats(id = id, isSolved = isSolved, attempts = attempts)
    }
}