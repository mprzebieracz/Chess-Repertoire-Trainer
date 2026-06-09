package com.example.chessrepertoiretrainer.core.chess.pgn.navigator

import com.example.chessrepertoiretrainer.core.database.entity.LineMove
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.move.Move
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GuidedLineNavigatorTest {

    private val STARTING_FEN = GuidedLineNavigator.STARTING_FEN
    private val FEN_AFTER_E4 = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1"
    private val FEN_AFTER_E5 = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR w KQkq e6 0 2"

    private fun lm(san: String, fen: String, comment: String? = null, index: Int = 0) =
        LineMove(
            id = 0,
            lineId = 1,
            moveIndex = index,
            moveSan = san,
            fen = fen,
            comment = comment,
            arrows = null
        )

    private fun twoMoveLine() = listOf(
        lm("e4", FEN_AFTER_E4, index = 0),
        lm("e5", FEN_AFTER_E5, index = 1),
    )

    @Test
    fun `initial state - isAtStart, currentMoveIndex is -1`() {
        val nav = GuidedLineNavigator(twoMoveLine())
        assertTrue(nav.isAtStart)
        assertFalse(nav.isAtEnd)
        assertEquals(-1, nav.currentMoveIndex)
        assertNull(nav.currentNodeId)
    }

    @Test
    fun `empty() creates navigator with no moves`() {
        val nav = GuidedLineNavigator.empty()
        assertTrue(nav.isEmpty)
        assertTrue(nav.isAtStart)
        assertTrue(nav.isAtEnd)
    }

    @Test
    fun `currentFen returns initialFen at start`() {
        val nav = GuidedLineNavigator(twoMoveLine())
        assertEquals(STARTING_FEN, nav.currentFen())
    }

    @Test
    fun `peekNextSan returns first move san at start`() {
        val nav = GuidedLineNavigator(twoMoveLine())
        assertEquals("e4", nav.peekNextSan())
    }

    @Test
    fun `peekNextSan returns null at end`() {
        val nav = GuidedLineNavigator(twoMoveLine())
        nav.onUserMove(AppliedMove(Move(Square.E2, Square.E4), "e4", STARTING_FEN, FEN_AFTER_E4))
        nav.onUserMove(AppliedMove(Move(Square.E7, Square.E5), "e5", FEN_AFTER_E4, FEN_AFTER_E5))
        assertNull(nav.peekNextSan())
    }

    @Test
    fun `onUserMove with correct SAN returns Accepted and advances index`() {
        val nav = GuidedLineNavigator(twoMoveLine())
        val result = nav.onUserMove(
            AppliedMove(
                Move(Square.E2, Square.E4),
                "e4",
                STARTING_FEN,
                FEN_AFTER_E4
            )
        )
        assertTrue(result is UserMoveResult.Accepted)
        assertEquals(0, nav.currentMoveIndex)
        assertFalse(nav.isAtStart)
    }

    @Test
    fun `onUserMove with wrong SAN returns Rejected and leaves index unchanged`() {
        val nav = GuidedLineNavigator(twoMoveLine())
        val result = nav.onUserMove(
            AppliedMove(
                Move(Square.D2, Square.D4),
                "d4",
                STARTING_FEN,
                FEN_AFTER_E4
            )
        )
        assertEquals(UserMoveResult.Rejected, result)
        assertEquals(-1, nav.currentMoveIndex)
        assertTrue(nav.isAtStart)
    }

    @Test
    fun `onUserMove at end of line returns Rejected`() {
        val nav = GuidedLineNavigator(listOf(lm("e4", FEN_AFTER_E4)))
        nav.onUserMove(AppliedMove(Move(Square.E2, Square.E4), "e4", STARTING_FEN, FEN_AFTER_E4))
        val result = nav.onUserMove(
            AppliedMove(
                Move(Square.E7, Square.E5),
                "e5",
                FEN_AFTER_E4,
                FEN_AFTER_E5
            )
        )
        assertEquals(UserMoveResult.Rejected, result)
    }

    @Test
    fun `isAtEnd true after all moves played`() {
        val nav = GuidedLineNavigator(listOf(lm("e4", FEN_AFTER_E4)))
        nav.onUserMove(AppliedMove(Move(Square.E2, Square.E4), "e4", STARTING_FEN, FEN_AFTER_E4))
        assertTrue(nav.isAtEnd)
    }

    @Test
    fun `currentFen returns fenAfter after a move`() {
        val nav = GuidedLineNavigator(twoMoveLine())
        nav.onUserMove(AppliedMove(Move(Square.E2, Square.E4), "e4", STARTING_FEN, FEN_AFTER_E4))
        assertEquals(FEN_AFTER_E4, nav.currentFen())
    }

    @Test
    fun `goPrevious after one move returns to root`() {
        val nav = GuidedLineNavigator(twoMoveLine())
        nav.onUserMove(AppliedMove(Move(Square.E2, Square.E4), "e4", STARTING_FEN, FEN_AFTER_E4))
        assertTrue(nav.goPrevious())
        assertEquals(-1, nav.currentMoveIndex)
        assertTrue(nav.isAtStart)
        assertEquals(STARTING_FEN, nav.currentFen())
    }

    @Test
    fun `goPrevious at start returns false`() {
        val nav = GuidedLineNavigator(twoMoveLine())
        assertFalse(nav.goPrevious())
    }

    @Test
    fun `goNext advances from start to first move`() {
        val nav = GuidedLineNavigator(twoMoveLine())
        assertTrue(nav.goNext())
        assertEquals(0, nav.currentMoveIndex)
        assertEquals(FEN_AFTER_E4, nav.currentFen())
    }

    @Test
    fun `goNext at end returns false`() {
        val nav = GuidedLineNavigator(listOf(lm("e4", FEN_AFTER_E4)))
        nav.goNext()
        assertFalse(nav.goNext())
    }

    @Test
    fun `goTo with valid id navigates to that node`() {
        val nav = GuidedLineNavigator(twoMoveLine())
        nav.goNext()
        val id = nav.currentNodeId!!
        nav.goNext()
        assertTrue(nav.goTo(id))
        assertEquals(id, nav.currentNodeId)
        assertEquals(0, nav.currentMoveIndex)
    }

    @Test
    fun `goTo with invalid id returns false`() {
        val nav = GuidedLineNavigator(twoMoveLine())
        assertFalse(nav.goTo(MoveNodeId(999)))
    }

    @Test
    fun `goTo invokes onPositionChanged with correct fen`() {
        val nav = GuidedLineNavigator(twoMoveLine())
        nav.goNext()
        nav.goNext()
        val firstNodeId = MoveNodeId(0)
        var capturedFen: String? = null
        nav.onPositionChanged = { fen, _ -> capturedFen = fen }
        nav.goTo(firstNodeId)
        assertEquals(FEN_AFTER_E4, capturedFen)
    }

    @Test
    fun `comment on line move is accessible via currentComment`() {
        val moves = listOf(lm("e4", FEN_AFTER_E4, comment = "Best move"))
        val nav = GuidedLineNavigator(moves)
        nav.goNext()
        assertEquals("Best move", nav.currentComment)
    }

    @Test
    fun `currentComment is null when no comment on move`() {
        val nav = GuidedLineNavigator(twoMoveLine())
        nav.goNext()
        assertNull(nav.currentComment)
    }

    @Test
    fun `tree is built correctly from line moves`() {
        val nav = GuidedLineNavigator(twoMoveLine())
        assertEquals(1, nav.tree.rootChildren.size)
        assertEquals(2, nav.tree.nodes.size)
    }
}