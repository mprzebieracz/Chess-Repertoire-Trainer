package com.example.chessrepertoiretrainer.feature.repertoire.data

import com.example.chessrepertoiretrainer.core.database.dao.RepertoireDao
import com.example.chessrepertoiretrainer.core.database.entity.Chapter
import com.example.chessrepertoiretrainer.core.database.entity.Line
import com.example.chessrepertoiretrainer.core.database.entity.LineMove
import com.example.chessrepertoiretrainer.core.database.entity.Repertoire
import com.example.chessrepertoiretrainer.feature.repertoire.domain.RepertoireRepository
import kotlinx.coroutines.flow.Flow

class DefaultRepertoireRepository(private val repertoireDao: RepertoireDao) : RepertoireRepository {

    private val pgnImporter: PgnImporter by lazy { PgnImporter(repertoireDao) }

    override fun getAllRepertoires(): Flow<List<Repertoire>> = repertoireDao.getAllRepertoires()

    override suspend fun getRepertoireById(id: Int): Repertoire? =
        repertoireDao.getRepertoireById(id)

    override suspend fun insertRepertoire(repertoire: Repertoire): Long =
        repertoireDao.insertRepertoire(repertoire)

    override suspend fun updateRepertoire(repertoire: Repertoire) =
        repertoireDao.updateRepertoire(repertoire)

    override suspend fun deleteRepertoire(repertoire: Repertoire) =
        repertoireDao.deleteRepertoire(repertoire)

    override fun getChaptersForRepertoire(repertoireId: Int): Flow<List<Chapter>> =
        repertoireDao.getChaptersForRepertoire(repertoireId)

    override suspend fun getChapterById(id: Int): Chapter? = repertoireDao.getChapterById(id)

    override suspend fun insertChapter(chapter: Chapter): Long =
        repertoireDao.insertChapter(chapter)

    override suspend fun deleteChapter(chapter: Chapter) = repertoireDao.deleteChapter(chapter)

    override fun getLinesForChapter(chapterId: Int): Flow<List<Line>> =
        repertoireDao.getLinesForChapter(chapterId)

    override suspend fun getLineById(id: Int): Line? = repertoireDao.getLineById(id)

    override suspend fun getLineCountForChapter(chapterId: Int): Int =
        repertoireDao.getLineCountForChapter(chapterId)

    override suspend fun getLearnedLineCountForChapter(chapterId: Int): Int =
        repertoireDao.getLearnedLineCountForChapter(chapterId)

    override suspend fun insertLine(line: Line): Long = repertoireDao.insertLine(line)

    override suspend fun updateLine(line: Line) = repertoireDao.updateLine(line)

    override suspend fun deleteLine(line: Line) = repertoireDao.deleteLine(line)

    override fun getLinesToReview(currentTime: Long): Flow<List<Line>> =
        repertoireDao.getLinesToReview(currentTime)

    override suspend fun getLineCountForRepertoire(repertoireId: Int): Int =
        repertoireDao.getLineCountForRepertoire(repertoireId)

    override suspend fun getLearnedLineCountForRepertoire(repertoireId: Int): Int =
        repertoireDao.getLearnedLineCountForRepertoire(repertoireId)

    override fun getMovesForLine(lineId: Int): Flow<List<LineMove>> =
        repertoireDao.getMovesForLine(lineId)

    override suspend fun insertLineMove(move: LineMove): Long = repertoireDao.insertLineMove(move)

    override suspend fun updateLineMove(move: LineMove) = repertoireDao.updateLineMove(move)

    override suspend fun deleteLineMove(move: LineMove) = repertoireDao.deleteLineMove(move)

    override suspend fun updateChapter(chapter: Chapter) = repertoireDao.updateChapter(chapter)

    override suspend fun getLinesForChapters(chapterIds: List<Int>): List<Line> =
        repertoireDao.getLinesForChapters(chapterIds)

    override suspend fun importPgnToChapter(pgnString: String, chapterId: Int) {
        pgnImporter.importPgnToChapter(pgnString = pgnString, chapterId = chapterId)
    }

    override suspend fun getAllLineMoveFens(): Map<Int, List<String>> =
        repertoireDao.getAllLineMoveFens().groupBy({ it.lineId }, { it.fen })

    override suspend fun updateLineEcoCode(lineId: Int, ecoCode: String?) =
        repertoireDao.updateLastEcoCode(lineId, ecoCode)
}