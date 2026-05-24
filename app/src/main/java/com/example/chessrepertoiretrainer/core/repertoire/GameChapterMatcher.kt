package com.example.chessrepertoiretrainer.core.repertoire

import com.example.chessrepertoiretrainer.core.chess.pgn.extract.PGNExtractor
import com.example.chessrepertoiretrainer.core.database.dao.SavedGameRepertoireMatchDao
import com.example.chessrepertoiretrainer.core.database.entity.SavedGame
import com.example.chessrepertoiretrainer.core.database.entity.SavedGameRepertoireMatch
import com.example.chessrepertoiretrainer.feature.mygames.domain.model.ComplianceStatus
import com.example.chessrepertoiretrainer.feature.repertoire.domain.usecase.RepertoireComplianceAnalyzer

class GameChapterMatcher(
    private val complianceAnalyzer: RepertoireComplianceAnalyzer,
    private val matchDao: SavedGameRepertoireMatchDao
) {

    suspend fun matchAndStore(game: SavedGame) {
        val isPlayerWhite = game.isPlayerWhite
        val index = complianceAnalyzer.buildIndex(isPlayerWhite)

        val sans = PGNExtractor.extractSanMovesFromPgn(game.pgn)
        if (sans.isEmpty()) return

        val anns = complianceAnalyzer.annotate(sans, isPlayerWhite, index)

        val playerMoveStatuses = setOf(
            ComplianceStatus.IN_BOOK,
            ComplianceStatus.DEVIATION,
            ComplianceStatus.OUT_OF_BOOK
        )
        val playerAnns = anns.filter { it.status in playerMoveStatuses }
        val playerTotal = playerAnns.size
        val playerInBook = playerAnns.count { it.status == ComplianceStatus.IN_BOOK }

        val deviationIdx = anns.indexOfFirst { it.status == ComplianceStatus.DEVIATION }
            .takeIf { it >= 0 }
        val lastInBook = anns.indexOfLast {
            it.status == ComplianceStatus.IN_BOOK || it.status == ComplianceStatus.OPPONENT_IN_BOOK
        }

        val deepestAnnotation = if (lastInBook >= 0) {
            anns.take(lastInBook + 1).lastOrNull { it.chapterIdForNavigation != null }
        } else null

        matchDao.upsertMatch(
            SavedGameRepertoireMatch(
                gameId = game.id,
                color = if (isPlayerWhite) "White" else "Black",
                deepestChapterId = deepestAnnotation?.chapterIdForNavigation,
                deepestLineId = deepestAnnotation?.lineIdForNavigation,
                lastInBookMoveIndex = lastInBook,
                deviationMoveIndex = deviationIdx,
                playerInBookMoves = playerInBook,
                playerTotalMoves = playerTotal,
                playerDeviated = deviationIdx != null,
                bookDepthPlies = (lastInBook + 1).coerceAtLeast(0),
                computedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun matchAllGames(games: List<SavedGame>) {
        val whiteIndex = complianceAnalyzer.buildIndex(playerIsWhite = true)
        val blackIndex = complianceAnalyzer.buildIndex(playerIsWhite = false)

        games.forEach { game ->
            val index = if (game.isPlayerWhite) whiteIndex else blackIndex
            val sans = PGNExtractor.extractSanMovesFromPgn(game.pgn)
            if (sans.isEmpty()) return@forEach

            val anns = complianceAnalyzer.annotate(sans, game.isPlayerWhite, index)

            val playerMoveStatuses = setOf(
                ComplianceStatus.IN_BOOK,
                ComplianceStatus.DEVIATION,
                ComplianceStatus.OUT_OF_BOOK
            )
            val playerAnns = anns.filter { it.status in playerMoveStatuses }
            val playerInBook = playerAnns.count { it.status == ComplianceStatus.IN_BOOK }
            val deviationIdx = anns.indexOfFirst { it.status == ComplianceStatus.DEVIATION }
                .takeIf { it >= 0 }
            val lastInBook = anns.indexOfLast {
                it.status == ComplianceStatus.IN_BOOK || it.status == ComplianceStatus.OPPONENT_IN_BOOK
            }
            val deepestAnnotation = if (lastInBook >= 0) {
                anns.take(lastInBook + 1).lastOrNull { it.chapterIdForNavigation != null }
            } else null

            matchDao.upsertMatch(
                SavedGameRepertoireMatch(
                    gameId = game.id,
                    color = if (game.isPlayerWhite) "White" else "Black",
                    deepestChapterId = deepestAnnotation?.chapterIdForNavigation,
                    deepestLineId = deepestAnnotation?.lineIdForNavigation,
                    lastInBookMoveIndex = lastInBook,
                    deviationMoveIndex = deviationIdx,
                    playerInBookMoves = playerInBook,
                    playerTotalMoves = playerAnns.size,
                    playerDeviated = deviationIdx != null,
                    bookDepthPlies = (lastInBook + 1).coerceAtLeast(0),
                    computedAt = System.currentTimeMillis()
                )
            )
        }
    }
}
