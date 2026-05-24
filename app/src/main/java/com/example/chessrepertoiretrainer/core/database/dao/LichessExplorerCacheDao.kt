package com.example.chessrepertoiretrainer.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.chessrepertoiretrainer.core.database.entity.LichessExplorerCache

@Dao
interface LichessExplorerCacheDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: LichessExplorerCache)

    @Query("SELECT * FROM lichess_explorer_cache WHERE cacheKey = :cacheKey LIMIT 1")
    suspend fun getByKey(cacheKey: String): LichessExplorerCache?

    @Query("DELETE FROM lichess_explorer_cache WHERE fetchedAt < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)
}
