package com.example.chessrepertoiretrainer.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.chessrepertoiretrainer.core.database.entity.SavedGameRepertoireMatch

@Dao
interface SavedGameRepertoireMatchDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMatch(match: SavedGameRepertoireMatch)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMatches(matches: List<SavedGameRepertoireMatch>)

    @Query("SELECT * FROM saved_game_repertoire_match WHERE gameId = :gameId LIMIT 1")
    suspend fun getMatchForGame(gameId: String): SavedGameRepertoireMatch?

    @Query("SELECT * FROM saved_game_repertoire_match WHERE deepestChapterId = :chapterId")
    suspend fun getMatchesByChapter(chapterId: Int): List<SavedGameRepertoireMatch>

    @Query("SELECT COUNT(*) FROM saved_game_repertoire_match")
    suspend fun count(): Int

    @Query("DELETE FROM saved_game_repertoire_match")
    suspend fun deleteAll()
}
