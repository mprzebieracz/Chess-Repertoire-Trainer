package com.example.chessrepertoiretrainer.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.chessrepertoiretrainer.core.database.entity.Puzzle

@Dao
interface PuzzleDao {

    @Query("SELECT * FROM puzzles WHERE sourceDate = :date LIMIT 1")
    suspend fun getPuzzleByDate(date: String): Puzzle?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPuzzle(puzzle: Puzzle)

    @Query("UPDATE puzzles SET isSolved = :isSolved, attempts = :attempts WHERE id = :id")
    suspend fun updatePuzzleStats(id: String, isSolved: Boolean, attempts: Int)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPuzzles(puzzles: List<Puzzle>)

    @Query("SELECT * FROM puzzles WHERE source = 'opening' ORDER BY openingFamily ASC, rating ASC")
    suspend fun getOpeningPuzzles(): List<Puzzle>

    @Query("SELECT * FROM puzzles WHERE id = :id LIMIT 1")
    suspend fun getPuzzleById(id: String): Puzzle?

    @Query("DELETE FROM puzzles WHERE source = 'opening' AND openingFamily = :family AND isSolved = 0")
    suspend fun deleteUnsolvedOpeningPuzzlesByFamily(family: String)

    @Query("SELECT * FROM puzzles WHERE source = 'opening' AND openingFamily = :family AND isSolved = 0 ORDER BY rating ASC")
    suspend fun getUnsolvedByFamily(family: String): List<Puzzle>

    @Query("SELECT COUNT(*) FROM puzzles WHERE source = 'opening' AND openingFamily = :family AND isSolved = 0")
    suspend fun countUnsolvedByFamily(family: String): Int
}