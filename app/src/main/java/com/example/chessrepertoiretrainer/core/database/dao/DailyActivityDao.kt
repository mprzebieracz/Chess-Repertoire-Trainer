package com.example.chessrepertoiretrainer.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.chessrepertoiretrainer.core.database.entity.DailyActivity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyActivityDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertActivity(activity: DailyActivity)

    @Query("SELECT * FROM daily_activity WHERE date >= :from AND date <= :to ORDER BY date ASC")
    fun getRange(from: Long, to: Long): Flow<List<DailyActivity>>

    @Query("SELECT * FROM daily_activity ORDER BY date DESC")
    suspend fun getAll(): List<DailyActivity>

    @Query("SELECT * FROM daily_activity WHERE date = :date LIMIT 1")
    suspend fun getByDate(date: Long): DailyActivity?
}
