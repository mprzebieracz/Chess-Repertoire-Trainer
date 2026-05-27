package com.example.chessrepertoiretrainer.feature.repertoire.data.pgn

import com.example.chessrepertoiretrainer.core.chess.pgn.parse.PgnLineResolver
import com.example.chessrepertoiretrainer.core.database.dao.RepertoireDao
import com.example.chessrepertoiretrainer.core.database.entity.Line
import com.example.chessrepertoiretrainer.core.database.entity.LineMove

class PgnImportWriter(private val repertoireDao: RepertoireDao) {

    suspend fun insertLineWithMoves(
        chapterId: Int,
        lineName: String,
        moves: List<PgnLineResolver.ResolvedMove>
    ): Int {
        val existingCount = repertoireDao.getLineCountForChapter(chapterId)
        val line = Line(
            chapterId = chapterId,
            name = lineName,
            nextReviewDate = System.currentTimeMillis(),
            interval = 0,
            easeFactor = 2.5f,
            consecutiveCorrect = 0,
            sortOrder = existingCount
        )
        val movePlaceholders = moves.map { move ->
            LineMove(
                lineId = 0,
                moveIndex = 0,
                moveSan = move.san,
                fen = move.fen,
                comment = move.comment,
                arrows = null
            )
        }
        return repertoireDao.insertLineWithMoves(line, movePlaceholders)
    }
}
