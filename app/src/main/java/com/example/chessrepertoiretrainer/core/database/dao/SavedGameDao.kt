package com.example.chessrepertoiretrainer.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.chessrepertoiretrainer.core.database.entity.SavedGame
import kotlinx.coroutines.flow.Flow

data class GameStatsRaw(val played: Int, val wins: Int, val losses: Int, val draws: Int)

data class RatingPeakRaw(val rating: Int, val playedAt: Long)

data class ChapterStatsRaw(
    val played: Int,
    val wins: Int,
    val losses: Int,
    val draws: Int,
    val gamesInBook: Int,
    val gamesDeviated: Int,
    val avgBookDepthPlies: Double?
)

data class OpeningStatsRaw(
    val ecoCode: String,
    val played: Int,
    val wins: Int,
    val losses: Int,
    val draws: Int,
    val gamesInBook: Int,
    val gamesDeviated: Int
)

@Dao
interface SavedGameDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertGames(games: List<SavedGame>)

    @Query("SELECT * FROM saved_games")
    suspend fun getAllGames(): List<SavedGame>

    @Query("SELECT MAX(playedAt) FROM saved_games WHERE platform = :platform AND playerUsername = :username")
    suspend fun getLatestPlayedAt(platform: String, username: String): Long?

    @Query("SELECT COUNT(*) FROM saved_games WHERE platform = :platform AND playerUsername = :username")
    suspend fun countGames(platform: String, username: String): Int

    @Query(
        """
        SELECT * FROM saved_games
        WHERE (:platform IS NULL OR platform = :platform)
        AND (:isWhite IS NULL OR isPlayerWhite = :isWhite)
        AND (:ratedOnly = 0 OR rated = 1)
        ORDER BY playedAt DESC
    """
    )
    fun getAllGamesFiltered(
        platform: String?,
        isWhite: Boolean?,
        ratedOnly: Boolean
    ): Flow<List<SavedGame>>

    @Query(
        """
        SELECT COUNT(*) AS played,
        SUM(CASE WHEN playerResult = 'win'  THEN 1 ELSE 0 END) AS wins,
        SUM(CASE WHEN playerResult = 'loss' THEN 1 ELSE 0 END) AS losses,
        SUM(CASE WHEN playerResult = 'draw' THEN 1 ELSE 0 END) AS draws
        FROM saved_games
        WHERE playerUsername = :username AND platform = :platform
        AND (:category IS NULL OR timeCategory = :category)
        AND (:isWhite IS NULL OR isPlayerWhite = :isWhite)
        AND playedAt >= :since
    """
    )
    suspend fun getStatsRaw(
        username: String,
        platform: String,
        category: String?,
        isWhite: Boolean?,
        since: Long
    ): GameStatsRaw

    @Query(
        """
        SELECT playerRating FROM saved_games
        WHERE playerUsername = :username AND platform = :platform
        AND timeCategory = :category AND playerRating IS NOT NULL
        ORDER BY playedAt DESC LIMIT 1
    """
    )
    suspend fun getCurrentRating(username: String, platform: String, category: String): Int?

    @Query(
        """
        SELECT playerRating FROM saved_games
        WHERE playerUsername = :username AND platform = :platform
        AND timeCategory = :category AND playerRating IS NOT NULL AND playedAt >= :since
        ORDER BY playedAt ASC LIMIT 1
    """
    )
    suspend fun getRatingAtStartOfPeriod(
        username: String,
        platform: String,
        category: String,
        since: Long
    ): Int?

    @Query(
        """
        SELECT playerRating AS rating, playedAt FROM saved_games
        WHERE playerUsername = :username AND platform = :platform
        AND timeCategory = :category AND playerRating IS NOT NULL
        ORDER BY playerRating DESC LIMIT 1
    """
    )
    suspend fun getPeakRating(username: String, platform: String, category: String): RatingPeakRaw?

    @Query(
        """
        SELECT AVG(opponentRating) FROM saved_games
        WHERE playerUsername = :username AND platform = :platform
        AND (:category IS NULL OR timeCategory = :category)
        AND opponentRating IS NOT NULL AND playedAt >= :since
    """
    )
    suspend fun getAvgOpponentRating(
        username: String,
        platform: String,
        category: String?,
        since: Long
    ): Double?

    @Query("SELECT * FROM saved_games WHERE id = :id")
    suspend fun getGameById(id: String): SavedGame?

    @Query("SELECT COUNT(*) FROM saved_game_repertoire_match")
    fun observeMatchCount(): Flow<Int>

    @Query(
        """
        SELECT
            COUNT(*) AS played,
            SUM(CASE WHEN sg.playerResult = 'win'  THEN 1 ELSE 0 END) AS wins,
            SUM(CASE WHEN sg.playerResult = 'loss' THEN 1 ELSE 0 END) AS losses,
            SUM(CASE WHEN sg.playerResult = 'draw' THEN 1 ELSE 0 END) AS draws,
            SUM(CASE WHEN m.playerDeviated = 0 THEN 1 ELSE 0 END) AS gamesInBook,
            SUM(CASE WHEN m.playerDeviated = 1 THEN 1 ELSE 0 END) AS gamesDeviated,
            AVG(m.bookDepthPlies) AS avgBookDepthPlies
        FROM saved_games sg
        JOIN saved_game_repertoire_match m ON sg.id = m.gameId
        WHERE m.deepestChapterId = :chapterId
        AND (:isWhite IS NULL OR sg.isPlayerWhite = :isWhite)
        AND sg.playedAt >= :since
    """
    )
    suspend fun getStatsByChapter(
        chapterId: Int,
        isWhite: Boolean?,
        since: Long
    ): ChapterStatsRaw?

    @Query(
        """
        SELECT sg.ecoCode AS ecoCode,
            COUNT(*) AS played,
            SUM(CASE WHEN sg.playerResult = 'win'  THEN 1 ELSE 0 END) AS wins,
            SUM(CASE WHEN sg.playerResult = 'loss' THEN 1 ELSE 0 END) AS losses,
            SUM(CASE WHEN sg.playerResult = 'draw' THEN 1 ELSE 0 END) AS draws,
            SUM(CASE WHEN m.playerDeviated = 0 THEN 1 ELSE 0 END) AS gamesInBook,
            SUM(CASE WHEN m.playerDeviated = 1 THEN 1 ELSE 0 END) AS gamesDeviated
        FROM saved_games sg
        LEFT JOIN saved_game_repertoire_match m ON sg.id = m.gameId
        WHERE sg.ecoCode IS NOT NULL
        AND sg.playerUsername = :username AND sg.platform = :platform
        AND sg.playedAt >= :since
        GROUP BY sg.ecoCode
        ORDER BY played DESC
    """
    )
    suspend fun getStatsByOpening(
        username: String,
        platform: String,
        since: Long
    ): List<OpeningStatsRaw>

    @Query("SELECT * FROM saved_games WHERE ecoCode IS NULL")
    suspend fun getGamesWithNullEcoCode(): List<SavedGame>

    @Query("UPDATE saved_games SET ecoCode = :ecoCode WHERE id = :id")
    suspend fun updateEcoCode(id: String, ecoCode: String)
}