package com.example.chessrepertoiretrainer.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.chessrepertoiretrainer.core.database.entity.GameStats

@Dao
interface GameStatsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(stats: GameStats)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(stats: List<GameStats>)

    @Query("SELECT * FROM game_stats WHERE profileId = :profileId")
    suspend fun getStatsForProfile(profileId: Long): List<GameStats>

    @Query("DELETE FROM game_stats WHERE profileId = :profileId")
    suspend fun deleteForProfile(profileId: Long)
}

