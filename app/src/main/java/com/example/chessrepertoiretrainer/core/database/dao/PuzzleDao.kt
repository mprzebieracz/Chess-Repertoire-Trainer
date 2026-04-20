package com.example.chessrepertoiretrainer.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.chessrepertoiretrainer.core.database.entity.Puzzle
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

    @Query("SELECT COUNT(*) FROM puzzles WHERE isSolved = 0")
    suspend fun countUnsolvedPuzzles(): Int
}
