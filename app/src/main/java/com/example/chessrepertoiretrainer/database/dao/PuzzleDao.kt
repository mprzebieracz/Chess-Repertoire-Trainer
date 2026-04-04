package com.example.chessrepertoiretrainer.database.dao

import androidx.room.*
import com.example.chessrepertoiretrainer.database.entities.Puzzle
import kotlinx.coroutines.flow.Flow

@Dao
interface PuzzleDao {
    @Query("SELECT * FROM puzzles")
    fun getAllPuzzles(): Flow<List<Puzzle>>

    @Query("SELECT * FROM puzzles WHERE id = :id")
    suspend fun getPuzzleById(id: String): Puzzle?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPuzzle(puzzle: Puzzle)

    @Update
    suspend fun updatePuzzle(puzzle: Puzzle)

    @Delete
    suspend fun deletePuzzle(puzzle: Puzzle)

    @Query("SELECT * FROM puzzles WHERE isSolved = 0 ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomUnsolvedPuzzle(): Puzzle?

    @Query("UPDATE puzzles SET isSolved = :isSolved, attempts = :attempts WHERE id = :id")
    suspend fun updatePuzzleStats(id: String, isSolved: Boolean, attempts: Int)
}
