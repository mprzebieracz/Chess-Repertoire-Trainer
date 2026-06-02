package com.example.chessrepertoiretrainer.feature.puzzles

import com.example.chessrepertoiretrainer.core.database.entity.Puzzle
import com.example.chessrepertoiretrainer.core.database.entity.RepertoireOpening

interface PuzzleRepository {
    suspend fun getTodaysPuzzle(): Puzzle?
    suspend fun fetchAndSaveDailyPuzzle(): Puzzle?
    suspend fun markSolved(id: String, attempts: Int)
    suspend fun updateAttempts(id: String, attempts: Int)
    suspend fun getPuzzleById(id: String): Puzzle?
    suspend fun getOpeningPuzzles(): List<Puzzle>
    suspend fun fetchAndSaveOpeningPuzzles(openings: List<RepertoireOpening>)

    // Opening list
    suspend fun getRepertoireOpenings(): List<RepertoireOpening>
    suspend fun getOpeningByFamily(family: String): RepertoireOpening?
    suspend fun syncRepertoireOpenings(openings: List<RepertoireOpening>)

    // Session loading
    suspend fun getUnsolvedPuzzlesForFamilies(families: List<String>): List<Puzzle>
    suspend fun getNextUnsolvedPuzzleForFamilies(families: List<String>): Puzzle?
    suspend fun countUnsolvedForFamily(family: String): Int
}
