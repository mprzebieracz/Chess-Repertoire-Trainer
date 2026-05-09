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
}