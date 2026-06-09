package com.example.chessrepertoiretrainer.feature.repertoire.domain

import com.example.chessrepertoiretrainer.core.database.entity.Chapter
import com.example.chessrepertoiretrainer.core.database.entity.Line
import com.example.chessrepertoiretrainer.core.database.entity.LineMove
import com.example.chessrepertoiretrainer.core.database.entity.Repertoire
import kotlinx.coroutines.flow.Flow


interface RepertoireRepository {
    fun getAllRepertoires(): Flow<List<Repertoire>>
    suspend fun getRepertoireById(id: Int): Repertoire?
    suspend fun insertRepertoire(repertoire: Repertoire): Long
    suspend fun updateRepertoire(repertoire: Repertoire)
    suspend fun deleteRepertoire(repertoire: Repertoire)

    fun getChaptersForRepertoire(repertoireId: Int): Flow<List<Chapter>>
    suspend fun getChapterById(id: Int): Chapter?
    suspend fun insertChapter(chapter: Chapter): Long
    suspend fun deleteChapter(chapter: Chapter)

    fun getLinesForChapter(chapterId: Int): Flow<List<Line>>
    suspend fun getLineById(id: Int): Line?
    suspend fun getLineCountForChapter(chapterId: Int): Int
    suspend fun getLearnedLineCountForChapter(chapterId: Int): Int
    suspend fun insertLine(line: Line): Long
    suspend fun updateLine(line: Line)
    suspend fun deleteLine(line: Line)
    fun getLinesToReview(currentTime: Long): Flow<List<Line>>

    suspend fun getLineCountForRepertoire(repertoireId: Int): Int
    suspend fun getLearnedLineCountForRepertoire(repertoireId: Int): Int

    fun getMovesForLine(lineId: Int): Flow<List<LineMove>>
    suspend fun insertLineMove(move: LineMove): Long
    suspend fun updateLineMove(move: LineMove)
    suspend fun deleteLineMove(move: LineMove)

    suspend fun updateChapter(chapter: Chapter)
    suspend fun getLinesForChapters(chapterIds: List<Int>): List<Line>

    suspend fun importPgnToChapter(pgnString: String, chapterId: Int)

    suspend fun getAllLineMoveFens(): Map<Int, List<String>>
    suspend fun getAllLineColors(): Map<Int, String>
    suspend fun updateLineEcoCode(lineId: Int, ecoCode: String?)
    suspend fun updateLineImagePath(lineId: Int, imagePath: String?)
}

