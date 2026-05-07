package com.example.chessrepertoiretrainer.feature.mygames.data

import com.example.chessrepertoiretrainer.core.database.dao.GameStatsRaw
import com.example.chessrepertoiretrainer.core.database.entity.SavedGame
import kotlinx.coroutines.flow.Flow

interface SavedGameRepository {
    suspend fun insertGames(games: List<SavedGame>)
    suspend fun getLatestPlayedAt(platform: String, username: String): Long?
    suspend fun countGames(platform: String, username: String): Int
    fun getGamesFiltered(
        username: String,
        platform: String?,
        category: String?,
        result: String?,
        isWhite: Boolean?
    ): Flow<List<SavedGame>>
    suspend fun getStats(username: String, platform: String?, category: String?, since: Long): GameStatsRaw
    suspend fun getGameById(id: String): SavedGame?
}
