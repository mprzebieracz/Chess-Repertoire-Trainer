package com.example.chessrepertoiretrainer.feature.mygames.data

import com.example.chessrepertoiretrainer.core.database.dao.GameStatsRaw
import com.example.chessrepertoiretrainer.core.database.dao.RatingPeakRaw
import com.example.chessrepertoiretrainer.core.database.dao.SavedGameDao
import com.example.chessrepertoiretrainer.core.database.entity.SavedGame
import com.example.chessrepertoiretrainer.core.network.games.FetchedGame
import kotlinx.coroutines.flow.Flow

fun FetchedGame.toSavedGame(platform: String, username: String): SavedGame {
    val playerResult = when (result) {
        "1-0" -> if (isUserWhite) "win" else "loss"
        "0-1" -> if (isUserWhite) "loss" else "win"
        "1/2-1/2" -> "draw"
        else -> "unknown"
    }
    return SavedGame(
        id = "${platform}_${platformGameId}",
        platform = platform,
        platformGameId = platformGameId,
        playerUsername = username,
        opponentName = opponentName,
        isPlayerWhite = isUserWhite,
        result = result,
        playerResult = playerResult,
        timeControl = timeControl,
        timeCategory = timeCategory,
        opening = opening,
        playerRating = playerRating,
        opponentRating = opponentRating,
        rated = rated,
        playedAt = playedAt,
        pgn = pgn
    )
}

class DefaultSavedGameRepository(private val dao: SavedGameDao) : SavedGameRepository {

    override suspend fun insertGames(games: List<SavedGame>) = dao.insertGames(games)

    override suspend fun getLatestPlayedAt(platform: String, username: String): Long? =
        dao.getLatestPlayedAt(platform, username)

    override suspend fun countGames(platform: String, username: String): Int =
        dao.countGames(platform, username)

    override fun getAllGamesFiltered(
        platform: String?,
        isWhite: Boolean?
    ): Flow<List<SavedGame>> =
        dao.getAllGamesFiltered(platform, isWhite)

    override suspend fun getStats(
        username: String,
        platform: String,
        category: String?,
        isWhite: Boolean?,
        since: Long
    ): GameStatsRaw =
        dao.getStatsRaw(username, platform, category, isWhite, since)

    override suspend fun getCurrentRating(
        username: String,
        platform: String,
        category: String
    ): Int? =
        dao.getCurrentRating(username, platform, category)

    override suspend fun getRatingAtStartOfPeriod(
        username: String,
        platform: String,
        category: String,
        since: Long
    ): Int? =
        dao.getRatingAtStartOfPeriod(username, platform, category, since)

    override suspend fun getPeakRating(
        username: String,
        platform: String,
        category: String
    ): RatingPeakRaw? =
        dao.getPeakRating(username, platform, category)

    override suspend fun getAvgOpponentRating(
        username: String,
        platform: String,
        category: String?,
        since: Long
    ): Double? =
        dao.getAvgOpponentRating(username, platform, category, since)

    override suspend fun getGameById(id: String): SavedGame? = dao.getGameById(id)
}