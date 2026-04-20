package com.example.chessrepertoiretrainer.feature.stats.data

import com.example.chessrepertoiretrainer.core.database.entity.PlayerProfile
import com.example.chessrepertoiretrainer.feature.openingtree.data.GameWithPgn
import com.example.chessrepertoiretrainer.feature.openingtree.domain.GamesRepository
import com.example.chessrepertoiretrainer.feature.stats.domain.PlayerProfileRepository

/**
 * Handles profile resolution and game synchronization for stats-focused flows.
 */
class AccountSyncCoordinator(
    private val profileRepository: PlayerProfileRepository,
    private val gamesRepository: GamesRepository
) {

    data class SyncResult(
        val newGames: Int,
        val gamesCount: Int,
        val updatedLastSyncTime: Long?,
        val errorMessage: String?
    )

    suspend fun resolveOrCreateProfile(
        username: String, platform: String
    ): PlayerProfile? {
        val trimmed = username.trim()
        if (trimmed.isEmpty()) return null

        return try {
            profileRepository.getOrCreateProfile(trimmed, platform)
        }
        catch (_: Exception) {
            null
        }
    }

    suspend fun getStoredGamesCount(profileId: Long): Int {
        return try {
            gamesRepository.getGamesWithPgnForProfile(profileId).size
        }
        catch (_: Exception) {
            0
        }
    }

    suspend fun getGamesWithPgnForProfile(profileId: Long, maxGames: Int?): List<GameWithPgn> {
        return try {
            gamesRepository.getGamesWithPgnForProfile(profileId, maxGames)
        }
        catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun syncAccount(profileId: Long): SyncResult {
        return try {
            val sync = gamesRepository.syncGamesForProfile(profileId, null)
            val refreshedProfile = profileRepository.getProfileById(profileId)
            val gamesCount = getStoredGamesCount(profileId)

            SyncResult(
                newGames = sync.newGames,
                gamesCount = gamesCount,
                updatedLastSyncTime = refreshedProfile?.lastSyncTime,
                errorMessage = sync.errorMessage
            )
        }
        catch (e: Exception) {
            SyncResult(
                newGames = 0,
                gamesCount = 0,
                updatedLastSyncTime = null,
                errorMessage = e.message ?: "Unexpected error during sync"
            )
        }
    }
}

