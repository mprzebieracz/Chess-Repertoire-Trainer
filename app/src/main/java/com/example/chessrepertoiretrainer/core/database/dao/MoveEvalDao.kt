package com.example.chessrepertoiretrainer.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.chessrepertoiretrainer.core.database.entity.MoveEval

@Dao
interface MoveEvalDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEvals(evals: List<MoveEval>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEval(eval: MoveEval)

    @Query("SELECT * FROM move_eval WHERE gameId = :gameId ORDER BY moveIndex ASC")
    suspend fun getEvalsForGame(gameId: String): List<MoveEval>

    @Query("SELECT COUNT(*) > 0 FROM move_eval WHERE gameId = :gameId")
    suspend fun hasEvalsForGame(gameId: String): Boolean

    @Query("DELETE FROM move_eval WHERE gameId = :gameId")
    suspend fun deleteForGame(gameId: String)
}
