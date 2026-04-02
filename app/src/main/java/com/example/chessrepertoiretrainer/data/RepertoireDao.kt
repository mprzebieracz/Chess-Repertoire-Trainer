package com.example.chessrepertoiretrainer.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RepertoireDao {
    // Repertoire
    @Query("SELECT * FROM repertoires")
    fun getAllRepertoires(): Flow<List<Repertoire>>

    @Query("SELECT * FROM repertoires WHERE id = :id")
    suspend fun getRepertoireById(id: Int): Repertoire?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRepertoire(repertoire: Repertoire)

    @Delete
    suspend fun deleteRepertoire(repertoire: Repertoire)

    // Chapter
    @Query("SELECT * FROM chapters WHERE repertoireId = :repertoireId")
    fun getChaptersForRepertoire(repertoireId: Int): Flow<List<Chapter>>

    @Query("SELECT * FROM chapters WHERE id = :id")
    suspend fun getChapterById(id: Int): Chapter?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapter(chapter: Chapter): Long

    @Delete
    suspend fun deleteChapter(chapter: Chapter)

    // Line
    @Query("SELECT * FROM lines WHERE chapterId = :chapterId")
    fun getLinesForChapter(chapterId: Int): Flow<List<Line>>

    @Query("SELECT * FROM lines WHERE id = :id")
    suspend fun getLineById(id: Int): Line?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLine(line: Line): Long

    @Delete
    suspend fun deleteLine(line: Line)

    // LineMove
    @Query("SELECT * FROM line_moves WHERE lineId = :lineId ORDER BY moveIndex ASC")
    fun getMovesForLine(lineId: Int): Flow<List<LineMove>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLineMove(move: LineMove)

    @Query("DELETE FROM line_moves WHERE lineId = :lineId AND moveIndex >= :moveIndex")
    suspend fun deleteMovesFromIndex(lineId: Int, moveIndex: Int)

    @Query("SELECT MAX(moveIndex) FROM line_moves WHERE lineId = :lineId")
    suspend fun getMaxMoveIndexForLine(lineId: Int): Int?

    @Query("DELETE FROM line_moves WHERE id = (SELECT id FROM line_moves WHERE lineId = :lineId ORDER BY moveIndex DESC LIMIT 1)")
    suspend fun deleteLastMoveForLine(lineId: Int)

    // Legacy (to be removed if not used)
    @Query("""
        SELECT rm.* FROM line_moves rm
        INNER JOIN lines l ON rm.lineId = l.id
        INNER JOIN chapters c ON l.chapterId = c.id
        WHERE c.repertoireId = :repertoireId
    """)
    fun getMovesForRepertoire(repertoireId: Int): Flow<List<LineMove>>

    @Query("""
        SELECT r.* FROM repertoires r
        INNER JOIN chapters c ON r.id = c.repertoireId
        WHERE c.id = :chapterId
    """)
    fun getRepertoireForChapter(chapterId: Int): Flow<Repertoire?>
    
    // Legacy support for non-linear models that haven't been refactored yet
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMove(move: RepertoireMove): Long

    @Query("SELECT * FROM repertoire_moves WHERE chapterId = :chapterId")
    fun getMovesForChapter(chapterId: Int): Flow<List<RepertoireMove>>

    @Query("DELETE FROM repertoire_moves WHERE id = :moveId")
    suspend fun deleteMoveById(moveId: Long)
}
