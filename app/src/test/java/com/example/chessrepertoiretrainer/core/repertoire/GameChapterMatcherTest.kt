package com.example.chessrepertoiretrainer.core.repertoire

import com.example.chessrepertoiretrainer.core.database.dao.SavedGameRepertoireMatchDao
import com.example.chessrepertoiretrainer.core.database.entity.SavedGame
import com.example.chessrepertoiretrainer.core.database.entity.SavedGameRepertoireMatch
import com.example.chessrepertoiretrainer.feature.mygames.domain.model.ComplianceStatus
import com.example.chessrepertoiretrainer.feature.mygames.domain.model.MoveAnnotation
import com.example.chessrepertoiretrainer.feature.repertoire.domain.usecase.RepertoireComplianceAnalyzer
import com.example.chessrepertoiretrainer.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class GameChapterMatcherTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val analyzer = mockk<RepertoireComplianceAnalyzer>()
    private val matchDao = mockk<SavedGameRepertoireMatchDao>(relaxed = true)
    private val matcher = GameChapterMatcher(analyzer, matchDao)

    private fun game(isWhite: Boolean = true, pgn: String = "1. e4 e5 1-0") = SavedGame(
        id = "g1",
        platform = "lichess",
        platformGameId = "g1",
        playerUsername = "user",
        opponentName = "opp",
        isPlayerWhite = isWhite,
        result = "1-0",
        playerResult = "win",
        timeControl = "600",
        timeCategory = "rapid",
        opening = null,
        playerRating = 1500,
        opponentRating = 1500,
        rated = true,
        playedAt = 1000L,
        pgn = pgn
    )

    private fun ann(status: ComplianceStatus, idx: Int, chapterId: Int? = null, lineId: Int? = null) =
        MoveAnnotation(moveIndex = idx, status = status, playedSan = "e4", chapterIdForNavigation = chapterId, lineIdForNavigation = lineId)

    // ---- player/opponent split ----

    @Test
    fun `playerTotal counts only player-side moves`() = runTest {
        val anns = listOf(
            ann(ComplianceStatus.IN_BOOK, 0, chapterId = 1),
            ann(ComplianceStatus.OPPONENT_IN_BOOK, 1),
            ann(ComplianceStatus.IN_BOOK, 2, chapterId = 1),
            ann(ComplianceStatus.OPPONENT_DEVIATION, 3),
            ann(ComplianceStatus.DEVIATION, 4),
            ann(ComplianceStatus.OUT_OF_BOOK, 5)
        )
        coEvery { analyzer.buildIndex(true) } returns emptyMap()
        coEvery { analyzer.annotate(any(), true, any()) } returns anns

        val slot = slot<SavedGameRepertoireMatch>()
        coEvery { matchDao.upsertMatch(capture(slot)) } returns Unit

        matcher.matchAndStore(game(isWhite = true))

        assertEquals(4, slot.captured.playerTotalMoves)
    }

    @Test
    fun `playerInBookMoves counts only IN_BOOK annotations`() = runTest {
        val anns = listOf(
            ann(ComplianceStatus.IN_BOOK, 0, chapterId = 1),
            ann(ComplianceStatus.OPPONENT_IN_BOOK, 1),
            ann(ComplianceStatus.IN_BOOK, 2, chapterId = 1),
            ann(ComplianceStatus.DEVIATION, 4),
            ann(ComplianceStatus.OUT_OF_BOOK, 5)
        )
        coEvery { analyzer.buildIndex(true) } returns emptyMap()
        coEvery { analyzer.annotate(any(), true, any()) } returns anns

        val slot = slot<SavedGameRepertoireMatch>()
        coEvery { matchDao.upsertMatch(capture(slot)) } returns Unit

        matcher.matchAndStore(game(isWhite = true))

        assertEquals(2, slot.captured.playerInBookMoves)
    }

    @Test
    fun `deviationMoveIndex is list index of first DEVIATION`() = runTest {
        val anns = listOf(
            ann(ComplianceStatus.IN_BOOK, 0, chapterId = 1),
            ann(ComplianceStatus.OPPONENT_IN_BOOK, 1),
            ann(ComplianceStatus.DEVIATION, 2)
        )
        coEvery { analyzer.buildIndex(true) } returns emptyMap()
        coEvery { analyzer.annotate(any(), true, any()) } returns anns

        val slot = slot<SavedGameRepertoireMatch>()
        coEvery { matchDao.upsertMatch(capture(slot)) } returns Unit

        matcher.matchAndStore(game(isWhite = true))

        assertEquals(2, slot.captured.deviationMoveIndex)
    }

    @Test
    fun `deviationMoveIndex is null when no DEVIATION`() = runTest {
        val anns = listOf(
            ann(ComplianceStatus.IN_BOOK, 0, chapterId = 1),
            ann(ComplianceStatus.OPPONENT_IN_BOOK, 1)
        )
        coEvery { analyzer.buildIndex(true) } returns emptyMap()
        coEvery { analyzer.annotate(any(), true, any()) } returns anns

        val slot = slot<SavedGameRepertoireMatch>()
        coEvery { matchDao.upsertMatch(capture(slot)) } returns Unit

        matcher.matchAndStore(game(isWhite = true))

        assertNull(slot.captured.deviationMoveIndex)
        assertFalse(slot.captured.playerDeviated)
    }

    @Test
    fun `bookDepthPlies is lastInBook index plus one`() = runTest {
        val anns = listOf(
            ann(ComplianceStatus.IN_BOOK, 0, chapterId = 1),
            ann(ComplianceStatus.OPPONENT_IN_BOOK, 1),
            ann(ComplianceStatus.IN_BOOK, 2, chapterId = 1)    // lastInBook = index 2
        )
        coEvery { analyzer.buildIndex(true) } returns emptyMap()
        coEvery { analyzer.annotate(any(), true, any()) } returns anns

        val slot = slot<SavedGameRepertoireMatch>()
        coEvery { matchDao.upsertMatch(capture(slot)) } returns Unit

        matcher.matchAndStore(game(isWhite = true))

        assertEquals(3, slot.captured.bookDepthPlies)
    }

    @Test
    fun `bookDepthPlies is zero when no in-book moves`() = runTest {
        val anns = listOf(
            ann(ComplianceStatus.DEVIATION, 0),
            ann(ComplianceStatus.OUT_OF_BOOK, 1)
        )
        coEvery { analyzer.buildIndex(true) } returns emptyMap()
        coEvery { analyzer.annotate(any(), true, any()) } returns anns

        val slot = slot<SavedGameRepertoireMatch>()
        coEvery { matchDao.upsertMatch(capture(slot)) } returns Unit

        matcher.matchAndStore(game(isWhite = true))

        assertEquals(0, slot.captured.bookDepthPlies)
    }

    // ---- color field ----

    @Test
    fun `color is White when player is white`() = runTest {
        coEvery { analyzer.buildIndex(true) } returns emptyMap()
        coEvery { analyzer.annotate(any(), true, any()) } returns emptyList()

        val slot = slot<SavedGameRepertoireMatch>()
        coEvery { matchDao.upsertMatch(capture(slot)) } returns Unit

        matcher.matchAndStore(game(isWhite = true))
        assertEquals("White", slot.captured.color)
    }

    @Test
    fun `color is Black when player is black`() = runTest {
        coEvery { analyzer.buildIndex(false) } returns emptyMap()
        coEvery { analyzer.annotate(any(), false, any()) } returns emptyList()

        val slot = slot<SavedGameRepertoireMatch>()
        coEvery { matchDao.upsertMatch(capture(slot)) } returns Unit

        matcher.matchAndStore(game(isWhite = false))
        assertEquals("Black", slot.captured.color)
    }

    // ---- empty PGN ----

    @Test
    fun `empty PGN skips upsert`() = runTest {
        coEvery { analyzer.buildIndex(true) } returns emptyMap()

        matcher.matchAndStore(game(pgn = ""))

        coVerify(exactly = 0) { matchDao.upsertMatch(any()) }
    }
}
