package com.example.chessrepertoiretrainer.feature.mygames.data

import com.example.chessrepertoiretrainer.core.database.dao.GameStatsRaw
import com.example.chessrepertoiretrainer.core.database.dao.SavedGameDao
import com.example.chessrepertoiretrainer.core.database.entity.SavedGame
import com.example.chessrepertoiretrainer.feature.openingtree.data.fetcher.FetchedGame
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

    override fun getGamesFiltered(
        username: String, platform: String?, category: String?, result: String?, isWhite: Boolean?
    ): Flow<List<SavedGame>> =
        dao.getGamesFiltered(username, platform, category, result, isWhite)

    override suspend fun getStats(
        username: String, platform: String?, category: String?, since: Long
    ): GameStatsRaw = dao.getStatsRaw(username, platform, category, since)

    override suspend fun getGameById(id: String): SavedGame? = dao.getGameById(id)
}
