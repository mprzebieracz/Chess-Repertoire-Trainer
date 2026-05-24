package com.example.chessrepertoiretrainer.feature.puzzles.data

import com.example.chessrepertoiretrainer.core.database.dao.PuzzleDao
import com.example.chessrepertoiretrainer.core.database.dao.RepertoireOpeningDao
import com.example.chessrepertoiretrainer.core.database.entity.Puzzle
import com.example.chessrepertoiretrainer.core.database.entity.RepertoireOpening
import com.example.chessrepertoiretrainer.core.network.puzzles.LichessPuzzleService
import com.example.chessrepertoiretrainer.core.network.puzzles.LichessOpeningPuzzleService
import com.example.chessrepertoiretrainer.feature.puzzles.PuzzleRepository
import java.time.LocalDate

class DefaultPuzzleRepository(
    private val puzzleDao: PuzzleDao,
    private val openingDao: RepertoireOpeningDao,
) : PuzzleRepository {

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

    override suspend fun getPuzzleById(id: String): Puzzle? = puzzleDao.getPuzzleById(id)

    override suspend fun getOpeningPuzzles(): List<Puzzle> = puzzleDao.getOpeningPuzzles()

    override suspend fun fetchAndSaveOpeningPuzzles(families: List<String>) {
        val allPuzzles = mutableListOf<Puzzle>()
        for (family in families) {
            puzzleDao.deleteUnsolvedOpeningPuzzlesByFamily(family)
            allPuzzles += LichessOpeningPuzzleService.getPuzzlesByOpening(family, count = 5)
        }
        if (allPuzzles.isNotEmpty()) puzzleDao.insertPuzzles(allPuzzles)
    }

    override suspend fun getRepertoireOpenings(): List<RepertoireOpening> = openingDao.getAll()

    override suspend fun syncRepertoireOpenings(openings: List<RepertoireOpening>) {
        if (openings.isEmpty()) {
            openingDao.deleteAll()
            return
        }
        openingDao.upsertAll(openings)
        openingDao.deleteObsolete(openings.map { it.family })
    }

    override suspend fun getUnsolvedPuzzlesForFamilies(families: List<String>): List<Puzzle> =
        families.flatMap { puzzleDao.getUnsolvedByFamily(it) }

    override suspend fun countUnsolvedForFamily(family: String): Int =
        puzzleDao.countUnsolvedByFamily(family)
}
