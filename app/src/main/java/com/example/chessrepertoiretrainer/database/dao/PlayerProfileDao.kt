package com.example.chessrepertoiretrainer.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.chessrepertoiretrainer.database.entities.PlayerProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerProfileDao {

    @Query("SELECT * FROM player_profiles ORDER BY username")
    fun getAllProfiles(): Flow<List<PlayerProfile>>

    @Query("SELECT * FROM player_profiles WHERE id = :id")
    suspend fun getProfileById(id: Long): PlayerProfile?

    @Query(
        "SELECT * FROM player_profiles " +
            "WHERE username = :username AND platform = :platform LIMIT 1"
    )
    suspend fun getProfileByUsernameAndPlatform(
        username: String,
        platform: String
    ): PlayerProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: PlayerProfile): Long

    @Update
    suspend fun updateProfile(profile: PlayerProfile)

    @Delete
    suspend fun deleteProfile(profile: PlayerProfile)
}

