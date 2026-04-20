package com.example.chessrepertoiretrainer.data

import com.example.chessrepertoiretrainer.database.dao.PlayerProfileDao
import com.example.chessrepertoiretrainer.database.entities.PlayerProfile
import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing tracked player profiles used for opening-tree and
 * statistics features.
 */
class PlayerProfileRepository(
    private val dao: PlayerProfileDao
) {
    fun getAllProfiles(): Flow<List<PlayerProfile>> = dao.getAllProfiles()

    suspend fun getProfileById(id: Long): PlayerProfile? = dao.getProfileById(id)

    suspend fun getProfileByUsernameAndPlatform(
        username: String, platform: String
    ): PlayerProfile? {
        return dao.getProfileByUsernameAndPlatform(
            username.trim(), platform.trim().lowercase()
        )
    }

    suspend fun addOrUpdateProfile(
        username: String, platform: String, displayName: String? = null
    ): Long {
        val profile = PlayerProfile(
            username = username.trim(), platform = platform.trim().lowercase(), displayName = displayName?.trim()
        )
        return dao.insertProfile(profile)
    }

    /**
     * Helper used by the Your Games screen: find an existing profile for the
     * given username+platform, or create one if it does not exist yet.
     */
    suspend fun getOrCreateProfile(
        username: String, platform: String, displayName: String? = null
    ): PlayerProfile {
        val normalizedUsername = username.trim()
        val normalizedPlatform = platform.trim().lowercase()

        val existing = dao.getProfileByUsernameAndPlatform(normalizedUsername, normalizedPlatform)
        if (existing != null) return existing

        val newProfile = PlayerProfile(
            username = normalizedUsername, platform = normalizedPlatform, displayName = displayName?.trim()
        )
        val id = dao.insertProfile(newProfile)
        return newProfile.copy(id = id)
    }

    suspend fun deleteProfile(profile: PlayerProfile) {
        dao.deleteProfile(profile)
    }
}

