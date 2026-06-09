package com.example.chessrepertoiretrainer.feature.repertoire.domain.usecase

import com.example.chessrepertoiretrainer.core.chess.domain.moveFromSan
import com.example.chessrepertoiretrainer.core.database.dao.RepertoireDao
import com.example.chessrepertoiretrainer.core.database.dao.RepertoirePositionIndexDao
import com.example.chessrepertoiretrainer.core.database.entity.RepertoirePositionIndex
import com.example.chessrepertoiretrainer.feature.mygames.domain.model.ComplianceStatus
import com.example.chessrepertoiretrainer.feature.mygames.domain.model.MoveAnnotation
import com.github.bhlangonijr.chesslib.Board
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// fen → (chapterId, lineId)
typealias ComplianceIndex = Map<String, Pair<Int, Int>>

class RepertoireComplianceAnalyzer(
    private val repertoireDao: RepertoireDao,
    private val indexDao: RepertoirePositionIndexDao
) {

    suspend fun buildIndex(playerIsWhite: Boolean): ComplianceIndex = withContext(Dispatchers.IO) {
        val color = colorFor(playerIsWhite)
        if (indexDao.countForColor(color) == 0) {
            rebuildIndex(playerIsWhite)
        }
        else {
            indexDao.getForColor(color)
                .associate { it.normalizedFen to Pair(it.chapterId, it.lineId) }
        }
    }

    suspend fun rebuildIndex(playerIsWhite: Boolean): ComplianceIndex =
        withContext(Dispatchers.IO) {
            val color = colorFor(playerIsWhite)
            val rows = repertoireDao.getLineFensForColor(color)
            val seen = mutableSetOf<String>()
            val entries = mutableListOf<RepertoirePositionIndex>()
            val index = mutableMapOf<String, Pair<Int, Int>>()
            for (row in rows) {
                val fen = normalizeFen(row.fen)
                if (seen.add(fen)) {
                    entries.add(
                        RepertoirePositionIndex(
                            color = color,
                            normalizedFen = fen,
                            chapterId = row.chapterId,
                            lineId = row.lineId
                        )
                    )
                    index[fen] = Pair(row.chapterId, row.lineId)
                }
            }
            indexDao.deleteForColor(color)
            indexDao.insertAll(entries)
            index
        }

    fun annotate(
        sanMoves: List<String>,
        isPlayerWhite: Boolean,
        index: ComplianceIndex
    ): List<MoveAnnotation> {
        val board = Board()
        val result = mutableListOf<MoveAnnotation>()
        var hasDeviated = false
        var lastNavInfo: Pair<Int, Int>? = null

        for ((i, san) in sanMoves.withIndex()) {
            val isPlayerTurn = (i % 2 == 0) == isPlayerWhite
            val move = board.moveFromSan(san) ?: break
            board.doMove(move)
            val destFen = normalizeFen(board.fen)
            val navInfo = index[destFen]

            val status: ComplianceStatus
            val navTarget: Pair<Int, Int>?

            if (navInfo != null) {
                hasDeviated = false
                status =
                    if (isPlayerTurn) ComplianceStatus.IN_BOOK else ComplianceStatus.OPPONENT_IN_BOOK
                navTarget = navInfo
                lastNavInfo = navInfo
            }
            else if (!hasDeviated) {
                hasDeviated = true
                status =
                    if (isPlayerTurn) ComplianceStatus.DEVIATION else ComplianceStatus.OPPONENT_DEVIATION
                navTarget = if (isPlayerTurn) lastNavInfo else null
            }
            else {
                status = ComplianceStatus.OUT_OF_BOOK
                navTarget = null
            }

            result.add(MoveAnnotation(i, status, san, navTarget?.first, navTarget?.second))
        }
        return result
    }

    private fun colorFor(playerIsWhite: Boolean) = if (playerIsWhite) "White" else "Black"

    private fun normalizeFen(fen: String) = fen.split(" ").take(3).joinToString(" ")
}