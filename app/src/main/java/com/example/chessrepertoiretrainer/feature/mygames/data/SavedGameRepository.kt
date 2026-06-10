package com.example.chessrepertoiretrainer.feature.mygames.data

import com.example.chessrepertoiretrainer.core.database.dao.ChapterStatsRaw
import com.example.chessrepertoiretrainer.core.database.dao.GameStatsRaw
import com.example.chessrepertoiretrainer.core.database.dao.OpeningStatsRaw
import com.example.chessrepertoiretrainer.core.database.dao.RatingPeakRaw
import com.example.chessrepertoiretrainer.core.database.entity.SavedGame
import kotlinx.coroutines.flow.Flow

interface SavedGameRepository {
    suspend fun insertGames(games: List<SavedGame>)
    suspend fun getAllGames(): List<SavedGame>
    suspend fun getLatestPlayedAt(platform: String, username: String): Long?
    suspend fun countGames(platform: String, username: String): Int
    fun getAllGamesFiltered(
        platform: String?,
        isWhite: Boolean?,
        ratedOnly: Boolean
    ): Flow<List<SavedGame>>

    suspend fun getStats(
        username: String,
        platform: String,
        category: String?,
        isWhite: Boolean?,
        since: Long
    ): GameStatsRaw

    suspend fun getCurrentRating(username: String, platform: String, category: String): Int?
    suspend fun getRatingAtStartOfPeriod(
        username: String,
        platform: String,
        category: String,
        since: Long
    ): Int?

    suspend fun getPeakRating(username: String, platform: String, category: String): RatingPeakRaw?
    suspend fun getAvgOpponentRating(
        username: String,
        platform: String,
        category: String?,
        since: Long
    ): Double?

    suspend fun getGameById(id: String): SavedGame?

    suspend fun getStatsByChapter(chapterId: Int, isWhite: Boolean?, since: Long): ChapterStatsRaw?
    suspend fun getStatsByOpening(
        username: String,
        platform: String,
        since: Long
    ): List<OpeningStatsRaw>

    suspend fun getGamesWithNullEcoCode(): List<SavedGame>
    suspend fun updateEcoCode(id: String, ecoCode: String)

    fun observeMatchCount(): Flow<Int>
}