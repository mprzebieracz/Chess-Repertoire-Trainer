package com.example.chessrepertoiretrainer.feature.repertoire.domain.usecase

import com.example.chessrepertoiretrainer.core.chess.domain.moveFromSan
import com.example.chessrepertoiretrainer.feature.mygames.domain.model.ComplianceStatus
import com.example.chessrepertoiretrainer.feature.mygames.domain.model.MoveAnnotation
import com.github.bhlangonijr.chesslib.Board
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Test

class RepertoireComplianceAnalyzerTest {

    // DAO deps are only used by buildIndex/rebuildIndex — not by annotate()
    private val analyzer = RepertoireComplianceAnalyzer(mockk(relaxed = true), mockk(relaxed = true))

    // Mirrors the private normalizeFen in the analyzer: keeps first 3 FEN fields
    private fun normFen(fen: String) = fen.split(" ").take(3).joinToString(" ")

    private fun fenAfterMoves(vararg sans: String): String {
        val board = Board()
        for (san in sans) board.doMove(board.moveFromSan(san)!!)
        return normFen(board.fen)
    }

    // Builds an index with all positions after each prefix of the given SAN sequence
    private fun indexFrom(chapterId: Int = 1, lineId: Int = 1, vararg sans: String): ComplianceIndex {
        val map = mutableMapOf<String, Pair<Int, Int>>()
        for (i in 1..sans.size) {
            map[fenAfterMoves(*sans.take(i).toTypedArray())] = Pair(chapterId, lineId)
        }
        return map
    }

    // ---- normalizeFen (tested via annotate behavior) ----

    @Test
    fun `normalizeFen keeps 3 fields so clock variants of same position match`() {
        val fen1 = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1"
        val fen2 = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 5 20"
        assertEquals(normFen(fen1), normFen(fen2))
        assertEquals(3, normFen(fen1).split(" ").size)
    }

    // ---- annotate: player is White ----

    @Test
    fun `all moves in book - player IN_BOOK, opponent OPPONENT_IN_BOOK`() {
        val index = indexFrom(sans = arrayOf("e4", "e5", "Nf3", "Nc6"))
        val annotations = analyzer.annotate(listOf("e4", "e5", "Nf3", "Nc6"), isPlayerWhite = true, index = index)
        assertEquals(4, annotations.size)
        assertEquals(ComplianceStatus.IN_BOOK, annotations[0].status)          // e4 (White)
        assertEquals(ComplianceStatus.OPPONENT_IN_BOOK, annotations[1].status) // e5 (Black)
        assertEquals(ComplianceStatus.IN_BOOK, annotations[2].status)          // Nf3 (White)
        assertEquals(ComplianceStatus.OPPONENT_IN_BOOK, annotations[3].status) // Nc6 (Black)
    }

    @Test
    fun `player deviates at third move - DEVIATION then OUT_OF_BOOK`() {
        // Index has e4, e5 but not Nf3
        val index = indexFrom(sans = arrayOf("e4", "e5"))
        val annotations = analyzer.annotate(listOf("e4", "e5", "Nf3", "Nc6"), isPlayerWhite = true, index = index)
        assertEquals(ComplianceStatus.IN_BOOK, annotations[0].status)          // e4
        assertEquals(ComplianceStatus.OPPONENT_IN_BOOK, annotations[1].status) // e5
        assertEquals(ComplianceStatus.DEVIATION, annotations[2].status)        // Nf3 — first off-book
        assertEquals(ComplianceStatus.OUT_OF_BOOK, annotations[3].status)      // Nc6
    }

    @Test
    fun `deviation navTarget points to last in-book chapter and line`() {
        val chapterId = 7
        val lineId = 42
        val index = indexFrom(chapterId = chapterId, lineId = lineId, sans = arrayOf("e4", "e5"))
        val annotations = analyzer.annotate(listOf("e4", "e5", "Nf3"), isPlayerWhite = true, index = index)
        val dev = annotations[2]
        assertEquals(ComplianceStatus.DEVIATION, dev.status)
        assertEquals(chapterId, dev.chapterIdForNavigation)
        assertEquals(lineId, dev.lineIdForNavigation)
    }

    @Test
    fun `OUT_OF_BOOK moves have null navTarget`() {
        val index = indexFrom(sans = arrayOf("e4", "e5"))
        val annotations = analyzer.annotate(listOf("e4", "e5", "Nf3", "Nc6"), isPlayerWhite = true, index = index)
        assertNull(annotations[3].chapterIdForNavigation) // Nc6 out of book
        assertNull(annotations[3].lineIdForNavigation)
    }

    @Test
    fun `opponent deviates - OPPONENT_DEVIATION then player OUT_OF_BOOK`() {
        // Only e4 in index; e5 is off-book for Black
        val index = indexFrom(sans = arrayOf("e4"))
        val annotations = analyzer.annotate(listOf("e4", "e5", "Nf3"), isPlayerWhite = true, index = index)
        assertEquals(ComplianceStatus.IN_BOOK, annotations[0].status)             // e4
        assertEquals(ComplianceStatus.OPPONENT_DEVIATION, annotations[1].status)  // e5
        assertEquals(ComplianceStatus.OUT_OF_BOOK, annotations[2].status)         // Nf3
    }

    @Test
    fun `opponent deviation navTarget is null`() {
        val index = indexFrom(sans = arrayOf("e4"))
        val annotations = analyzer.annotate(listOf("e4", "e5"), isPlayerWhite = true, index = index)
        assertNull(annotations[1].chapterIdForNavigation)
        assertNull(annotations[1].lineIdForNavigation)
    }

    // ---- annotate: player is Black ----

    @Test
    fun `player is Black - odd indices are player moves`() {
        val index = indexFrom(sans = arrayOf("e4", "e5", "Nf3", "Nc6"))
        val annotations = analyzer.annotate(listOf("e4", "e5", "Nf3", "Nc6"), isPlayerWhite = false, index = index)
        assertEquals(ComplianceStatus.OPPONENT_IN_BOOK, annotations[0].status) // e4 (White = opponent)
        assertEquals(ComplianceStatus.IN_BOOK, annotations[1].status)          // e5 (Black = player)
        assertEquals(ComplianceStatus.OPPONENT_IN_BOOK, annotations[2].status) // Nf3 (White = opponent)
        assertEquals(ComplianceStatus.IN_BOOK, annotations[3].status)          // Nc6 (Black = player)
    }

    @Test
    fun `player is Black and deviates - correct DEVIATION status`() {
        // Index has e4, e5; Black plays c5 instead
        val index = indexFrom(sans = arrayOf("e4", "e5"))
        val annotations = analyzer.annotate(listOf("e4", "c5"), isPlayerWhite = false, index = index)
        assertEquals(ComplianceStatus.OPPONENT_IN_BOOK, annotations[0].status) // e4
        assertEquals(ComplianceStatus.DEVIATION, annotations[1].status)        // c5 (Black deviation)
    }

    // ---- annotate: transposition back into book ----

    @Test
    fun `transposition back into book resets deviation state`() {
        // e4 is in index (player IN_BOOK)
        // e5 NOT in index (opponent deviation)
        // same Nf3 FEN is in index (transposition) — should go back to IN_BOOK
        val board = Board()
        board.doMove(board.moveFromSan("e4")!!)
        board.doMove(board.moveFromSan("e5")!!)
        board.doMove(board.moveFromSan("Nf3")!!)
        val fenAfterNf3 = normFen(board.fen)

        val index = mutableMapOf<String, Pair<Int, Int>>()
        index[fenAfterMoves("e4")] = Pair(1, 1)
        index[fenAfterNf3] = Pair(1, 1)  // reachable via transposition

        val annotations = analyzer.annotate(listOf("e4", "e5", "Nf3"), isPlayerWhite = true, index = index)
        assertEquals(ComplianceStatus.IN_BOOK, annotations[0].status)            // e4
        assertEquals(ComplianceStatus.OPPONENT_DEVIATION, annotations[1].status)  // e5
        assertEquals(ComplianceStatus.IN_BOOK, annotations[2].status)             // Nf3 — transposition
    }

    // ---- annotate: edge cases ----

    @Test
    fun `empty move list returns empty annotations`() {
        val annotations = analyzer.annotate(emptyList(), isPlayerWhite = true, index = emptyMap())
        assertTrue(annotations.isEmpty())
    }

    @Test
    fun `empty index - first player move is DEVIATION, rest OUT_OF_BOOK`() {
        val annotations = analyzer.annotate(listOf("e4", "e5", "Nf3"), isPlayerWhite = true, index = emptyMap())
        assertEquals(ComplianceStatus.DEVIATION, annotations[0].status)
        // hasDeviated is already true after e4, so e5 and Nf3 are OUT_OF_BOOK (not OPPONENT_DEVIATION)
        assertEquals(ComplianceStatus.OUT_OF_BOOK, annotations[1].status)
        assertEquals(ComplianceStatus.OUT_OF_BOOK, annotations[2].status)
    }

    @Test
    fun `moveIndex in annotation matches position in input list`() {
        val index = indexFrom(sans = arrayOf("e4", "e5"))
        val annotations = analyzer.annotate(listOf("e4", "e5"), isPlayerWhite = true, index = index)
        assertEquals(0, annotations[0].moveIndex)
        assertEquals(1, annotations[1].moveIndex)
    }

    @Test
    fun `playedSan in annotation matches input san`() {
        val index = indexFrom(sans = arrayOf("e4", "e5"))
        val annotations = analyzer.annotate(listOf("e4", "e5"), isPlayerWhite = true, index = index)
        assertEquals("e4", annotations[0].playedSan)
        assertEquals("e5", annotations[1].playedSan)
    }
}
