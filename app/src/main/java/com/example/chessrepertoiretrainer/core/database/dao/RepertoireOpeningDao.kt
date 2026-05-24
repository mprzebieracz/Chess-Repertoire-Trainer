package com.example.chessrepertoiretrainer.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.chessrepertoiretrainer.core.database.entity.RepertoireOpening

@Dao
interface RepertoireOpeningDao {

    @Query("SELECT * FROM repertoire_openings ORDER BY family ASC")
    suspend fun getAll(): List<RepertoireOpening>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(openings: List<RepertoireOpening>)

    @Query("DELETE FROM repertoire_openings WHERE family NOT IN (:families)")
    suspend fun deleteObsolete(families: List<String>)

    @Query("DELETE FROM repertoire_openings")
    suspend fun deleteAll()
}
