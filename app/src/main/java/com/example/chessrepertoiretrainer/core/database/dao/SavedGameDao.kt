package com.example.chessrepertoiretrainer.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.chessrepertoiretrainer.core.database.entity.SavedGame
import kotlinx.coroutines.flow.Flow

data class GameStatsRaw(
    val played: Int,
    val wins: Int,
    val losses: Int,
    val draws: Int
)

@Dao
interface SavedGameDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertGames(games: List<SavedGame>)

    @Query("SELECT MAX(playedAt) FROM saved_games WHERE platform = :platform AND playerUsername = :username")
    suspend fun getLatestPlayedAt(platform: String, username: String): Long?

    @Query("SELECT COUNT(*) FROM saved_games WHERE platform = :platform AND playerUsername = :username")
    suspend fun countGames(platform: String, username: String): Int

    @Query("""
        SELECT * FROM saved_games
        WHERE playerUsername = :username
        AND (:platform IS NULL OR platform = :platform)
        AND (:category IS NULL OR timeCategory = :category)
        AND (:result IS NULL OR playerResult = :result)
        AND (:isWhite IS NULL OR isPlayerWhite = :isWhite)
        ORDER BY playedAt DESC
    """)
    fun getGamesFiltered(
        username: String,
        platform: String?,
        category: String?,
        result: String?,
        isWhite: Boolean?
    ): Flow<List<SavedGame>>

    @Query("""
        SELECT
            COUNT(*) AS played,
            SUM(CASE WHEN playerResult = 'win'  THEN 1 ELSE 0 END) AS wins,
            SUM(CASE WHEN playerResult = 'loss' THEN 1 ELSE 0 END) AS losses,
            SUM(CASE WHEN playerResult = 'draw' THEN 1 ELSE 0 END) AS draws
        FROM saved_games
        WHERE playerUsername = :username
        AND (:platform IS NULL OR platform = :platform)
        AND (:category IS NULL OR timeCategory = :category)
        AND playedAt >= :since
    """)
    suspend fun getStatsRaw(
        username: String,
        platform: String?,
        category: String?,
        since: Long
    ): GameStatsRaw

    @Query("SELECT * FROM saved_games WHERE id = :id")
    suspend fun getGameById(id: String): SavedGame?
}
