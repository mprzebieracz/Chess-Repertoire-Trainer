package com.example.chessrepertoiretrainer.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.chessrepertoiretrainer.core.database.entity.RepertoirePositionIndex

@Dao
interface RepertoirePositionIndexDao {

    @Query("SELECT * FROM repertoire_position_index WHERE color = :color")
    suspend fun getForColor(color: String): List<RepertoirePositionIndex>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<RepertoirePositionIndex>)

    @Query("DELETE FROM repertoire_position_index WHERE color = :color")
    suspend fun deleteForColor(color: String)

    @Query("SELECT COUNT(*) FROM repertoire_position_index WHERE color = :color")
    suspend fun countForColor(color: String): Int
}
