package com.example.chessrepertoiretrainer.feature.puzzles

import com.example.chessrepertoiretrainer.core.database.entity.Puzzle
import kotlinx.coroutines.flow.Flow

interface PuzzleRepository {

    fun getAllPuzzles(): Flow<List<Puzzle>>

    suspend fun getUnsolvedCount(): Int

    suspend fun ensureMinUnsolvedPuzzles(minUnsolved: Int): Int

    suspend fun getRandomUnsolvedPuzzle(): Puzzle?
    
    suspend fun updatePuzzleStats(id: String, isSolved: Boolean, attempts: Int)
}