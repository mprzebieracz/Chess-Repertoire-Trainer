package com.example.chessrepertoiretrainer.data

import com.example.chessrepertoiretrainer.database.entities.Puzzle
import kotlinx.coroutines.flow.Flow

/**
 * Abstraction over all puzzle-related data operations (Room + network).
 *
 * This allows ViewModels to stay focused on UI state and not depend directly
 * on Room DAO or the Lichess API service.
 */
interface PuzzleRepository {

    /** Flow of all stored puzzles. Useful for future overview screens. */
    fun getAllPuzzles(): Flow<List<Puzzle>>

    /** Current number of unsolved puzzles in the local database. */
    suspend fun getUnsolvedCount(): Int

    /**
     * Ensure there are at least [minUnsolved] unsolved puzzles available.
     *
     * Implementations may hit the network (e.g. Lichess) and insert newly
     * downloaded puzzles into the local database. Returns the final unsolved
     * count after any work is done.
     */
    suspend fun ensureMinUnsolvedPuzzles(minUnsolved: Int): Int

    /** Return a random unsolved puzzle, or null if none are available. */
    suspend fun getRandomUnsolvedPuzzle(): Puzzle?

    /** Update basic solved/attempts stats for a puzzle. */
    suspend fun updatePuzzleStats(id: String, isSolved: Boolean, attempts: Int)
}

