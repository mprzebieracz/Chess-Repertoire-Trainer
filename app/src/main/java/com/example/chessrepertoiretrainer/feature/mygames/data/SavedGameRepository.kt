package com.example.chessrepertoiretrainer.feature.mygames.data

import com.example.chessrepertoiretrainer.core.database.dao.GameStatsRaw
import com.example.chessrepertoiretrainer.core.database.dao.RatingPeakRaw
import com.example.chessrepertoiretrainer.core.database.entity.SavedGame
import kotlinx.coroutines.flow.Flow

interface SavedGameRepository {
    suspend fun insertGames(games: List<SavedGame>)
    suspend fun getLatestPlayedAt(platform: String, username: String): Long?
    suspend fun countGames(platform: String, username: String): Int
    fun getAllGamesFiltered(platform: String?,
                            isWhite: Boolean?): Flow<List<SavedGame>>

    suspend fun getStats(username: String,
                         platform: String,
                         category: String?,
                         isWhite: Boolean?,
                         since: Long): GameStatsRaw

    suspend fun getCurrentRating(username: String, platform: String, category: String): Int?
    suspend fun getRatingAtStartOfPeriod(username: String,
                                         platform: String,
                                         category: String,
                                         since: Long): Int?

    suspend fun getPeakRating(username: String, platform: String, category: String): RatingPeakRaw?
    suspend fun getAvgOpponentRating(username: String,
                                     platform: String,
                                     category: String?,
                                     since: Long): Double?

    suspend fun getGameById(id: String): SavedGame?
}