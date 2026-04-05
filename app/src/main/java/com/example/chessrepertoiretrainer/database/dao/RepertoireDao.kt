package com.example.chessrepertoiretrainer.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.chessrepertoiretrainer.database.entities.Chapter
import com.example.chessrepertoiretrainer.database.entities.Line
import com.example.chessrepertoiretrainer.database.entities.LineMove
import com.example.chessrepertoiretrainer.database.entities.Repertoire
import kotlinx.coroutines.flow.Flow

@Dao
interface RepertoireDao {
    // Repertoire
    @Query("SELECT * FROM repertoires")
    fun getAllRepertoires(): Flow<List<Repertoire>>

    @Query("SELECT * FROM repertoires WHERE id = :id")
    suspend fun getRepertoireById(id: Int): Repertoire?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRepertoire(repertoire: Repertoire): Long

    @Update
    suspend fun updateRepertoire(repertoire: Repertoire)

    @Delete
    suspend fun deleteRepertoire(repertoire: Repertoire)

    // Chapter
    @Query("SELECT * FROM chapters WHERE repertoireId = :repertoireId ORDER BY sortOrder ASC")
    fun getChaptersForRepertoire(repertoireId: Int): Flow<List<Chapter>>

    @Query("SELECT * FROM chapters WHERE id = :id")
    suspend fun getChapterById(id: Int): Chapter?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapter(chapter: Chapter): Long

    @Update
    suspend fun updateChapter(chapter: Chapter)

    @Delete
    suspend fun deleteChapter(chapter: Chapter)

    // Line
    @Query("SELECT * FROM lines WHERE chapterId = :chapterId")
    fun getLinesForChapter(chapterId: Int): Flow<List<Line>>

    @Query("SELECT COUNT(*) FROM lines WHERE chapterId = :chapterId")
    suspend fun getLineCountForChapter(chapterId: Int): Int

    @Query("SELECT * FROM lines WHERE id = :id")
    suspend fun getLineById(id: Int): Line?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLine(line: Line): Long

    @Update
    suspend fun updateLine(line: Line)

    @Delete
    suspend fun deleteLine(line: Line)

    @Query("SELECT * FROM lines WHERE nextReviewDate <= :currentTime")
    fun getLinesToReview(currentTime: Long): Flow<List<Line>>

    // LineMove
    @Query("SELECT * FROM line_moves WHERE lineId = :lineId ORDER BY moveIndex ASC")
    fun getMovesForLine(lineId: Int): Flow<List<LineMove>>

    @Query("SELECT * FROM line_moves WHERE fen = :fen")
    suspend fun getLineMovesByFen(fen: String): List<LineMove>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLineMove(move: LineMove): Long

    @Update
    suspend fun updateLineMove(move: LineMove)

    @Delete
    suspend fun deleteLineMove(move: LineMove)
}
