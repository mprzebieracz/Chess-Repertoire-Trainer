package com.example.chessrepertoiretrainer.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.chessrepertoiretrainer.core.database.entity.ReviewLog

@Dao
interface ReviewLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ReviewLog)

    @Query("SELECT * FROM review_log WHERE lineId = :lineId ORDER BY reviewedAt DESC")
    suspend fun getLogsForLine(lineId: Int): List<ReviewLog>

    @Query("SELECT COUNT(*) FROM review_log WHERE reviewedAt >= :since")
    suspend fun countSince(since: Long): Int
}
