package com.example.chessrepertoiretrainer.feature.stats.data.data

import com.example.chessrepertoiretrainer.feature.stats.domain.GameStatsRepository
import com.example.chessrepertoiretrainer.feature.stats.domain.GameStatsSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Encapsulates game-stats recomputation and summary loading for a profile.
 */
class StatsRefreshCoordinator(
    private val gameStatsRepository: GameStatsRepository
) {

    suspend fun recomputeAndLoadSummary(profileId: Long): GameStatsSummary? {
        return try {
            withContext(Dispatchers.Default) {
                gameStatsRepository.recomputeStatsForProfile(profileId)
            }
            withContext(Dispatchers.IO) {
                gameStatsRepository.loadSummaryForProfile(profileId)
            }
        }
        catch (_: Exception) {
            null
        }
    }
}

