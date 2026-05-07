package com.example.chessrepertoiretrainer.feature.puzzles

import com.example.chessrepertoiretrainer.core.database.entity.Puzzle

interface PuzzleRepository {
    suspend fun getTodaysPuzzle(): Puzzle?
    suspend fun fetchAndSaveDailyPuzzle(): Puzzle?
    suspend fun markSolved(id: String, attempts: Int)
    suspend fun updateAttempts(id: String, attempts: Int)
}
