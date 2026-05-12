package com.example.chessrepertoiretrainer.feature.puzzles.data

import com.example.chessrepertoiretrainer.core.database.dao.PuzzleDao
import com.example.chessrepertoiretrainer.core.database.entity.Puzzle
import com.example.chessrepertoiretrainer.core.network.puzzles.LichessPuzzleService
import com.example.chessrepertoiretrainer.feature.puzzles.PuzzleRepository
import java.time.LocalDate

class DefaultPuzzleRepository(private val puzzleDao: PuzzleDao) : PuzzleRepository {

    private fun today(): String = LocalDate.now().toString()

    override suspend fun getTodaysPuzzle(): Puzzle? = puzzleDao.getPuzzleByDate(today())

    override suspend fun fetchAndSaveDailyPuzzle(): Puzzle? {
        val puzzle = LichessPuzzleService.fetchDailyPuzzle(sourceDate = today()) ?: return null
        puzzleDao.insertPuzzle(puzzle)
        return puzzle
    }

    override suspend fun markSolved(id: String, attempts: Int) {
        puzzleDao.updatePuzzleStats(id = id, isSolved = true, attempts = attempts)
    }

    override suspend fun updateAttempts(id: String, attempts: Int) {
        puzzleDao.updatePuzzleStats(id = id, isSolved = false, attempts = attempts)
    }
}