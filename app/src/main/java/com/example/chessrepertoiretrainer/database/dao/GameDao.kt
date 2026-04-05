package com.example.chessrepertoiretrainer.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.chessrepertoiretrainer.database.entities.Game
import com.example.chessrepertoiretrainer.database.entities.GameMoves
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {

    @Query("SELECT * FROM games WHERE profileId = :profileId ORDER BY playedAt DESC")
    fun getGamesForProfile(profileId: Long): Flow<List<Game>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGame(game: Game): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGameMoves(moves: GameMoves)

    @Transaction
    suspend fun insertGameWithMoves(game: Game, moves: GameMoves) {
        val id = insertGame(game)
        insertGameMoves(moves.copy(gameId = id))
    }

    @Query("SELECT * FROM game_moves WHERE gameId = :gameId")
    suspend fun getGameMoves(gameId: Long): GameMoves?

    @Query("SELECT id FROM games WHERE profileId = :profileId AND platformGameId = :platformGameId LIMIT 1")
    suspend fun findGameId(profileId: Long, platformGameId: String): Long?
}

