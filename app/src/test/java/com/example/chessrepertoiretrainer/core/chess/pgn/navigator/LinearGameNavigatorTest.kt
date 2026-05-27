package com.example.chessrepertoiretrainer.core.chess.pgn.navigator

import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.move.Move
import org.junit.Assert.*
import org.junit.Test

class LinearGameNavigatorTest {

    private val STARTING_FEN = LinearGameNavigator.STARTING_FEN
    private val FEN_AFTER_E4 = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1"
    private val FEN_AFTER_D4 = "rnbqkbnr/pppppppp/8/8/3P4/8/PPP1PPPP/RNBQKBNR b KQkq d3 0 1"
    private val FEN_AFTER_E5 = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR w KQkq e6 0 2"

    private fun e4() = AppliedMove(Move(Square.E2, Square.E4), "e4", STARTING_FEN, FEN_AFTER_E4)
    private fun d4() = AppliedMove(Move(Square.D2, Square.D4), "d4", STARTING_FEN, FEN_AFTER_D4)
    private fun e5() = AppliedMove(Move(Square.E7, Square.E5), "e5", FEN_AFTER_E4, FEN_AFTER_E5)

    @Test
    fun `initial state - isAtStart and isAtEnd, currentMoveIndex is -1`() {
        val nav = LinearGameNavigator()
        assertTrue(nav.isAtStart)
        assertTrue(nav.isAtEnd)
        assertEquals(-1, nav.currentMoveIndex)
        assertNull(nav.currentNodeId)
    }

    @Test
    fun `initial tree is empty`() {
        val nav = LinearGameNavigator()
        assertTrue(nav.tree.rootChildren.isEmpty())
        assertTrue(nav.tree.nodes.isEmpty())
    }

    @Test
    fun `onUserMove appends to sanList and advances index`() {
        val nav = LinearGameNavigator()
        nav.onUserMove(e4())
        assertEquals(listOf("e4"), nav.sanList)
        assertEquals(0, nav.currentMoveIndex)
        assertTrue(nav.isAtEnd)
        assertFalse(nav.isAtStart)
    }

    @Test
    fun `onUserMove returns Accepted`() {
        val nav = LinearGameNavigator()
        val result = nav.onUserMove(e4())
        assertTrue(result is UserMoveResult.Accepted)
    }

    @Test
    fun `two onUserMove calls grow sanList to size 2`() {
        val nav = LinearGameNavigator()
        nav.onUserMove(e4())
        nav.onUserMove(e5())
        assertEquals(listOf("e4", "e5"), nav.sanList)
        assertEquals(1, nav.currentMoveIndex)
    }

    @Test
    fun `goPrevious after one move returns to root`() {
        val nav = LinearGameNavigator()
        nav.onUserMove(e4())
        assertTrue(nav.goPrevious())
        assertEquals(-1, nav.currentMoveIndex)
        assertTrue(nav.isAtStart)
        assertNull(nav.currentNodeId)
    }

    @Test
    fun `goPrevious at start returns false`() {
        val nav = LinearGameNavigator()
        assertFalse(nav.goPrevious())
    }

    @Test
    fun `goNext at end returns false`() {
        val nav = LinearGameNavigator()
        assertFalse(nav.goNext())
    }

    @Test
    fun `goNext after goPrevious advances forward`() {
        val nav = LinearGameNavigator()
        nav.onUserMove(e4())
        nav.goPrevious()
        assertTrue(nav.goNext())
        assertEquals(0, nav.currentMoveIndex)
        assertTrue(nav.isAtEnd)
    }

    @Test
    fun `new move while in middle trims future history`() {
        val nav = LinearGameNavigator()
        nav.onUserMove(e4())
        nav.onUserMove(e5())
        assertEquals(2, nav.sanList.size)
        // Go back to root
        nav.goPrevious()
        nav.goPrevious()
        // Play d4 instead — should discard e4 and e5
        nav.onUserMove(d4())
        assertEquals(listOf("d4"), nav.sanList)
    }

    @Test
    fun `new move mid-line trims only future moves not the prefix`() {
        val nav = LinearGameNavigator()
        nav.onUserMove(e4())
        nav.onUserMove(e5())
        nav.goPrevious()  // back to after e4
        // Play d4 instead of e5 — trims e5
        val d4fromE4 = AppliedMove(Move(Square.D2, Square.D4), "d4", FEN_AFTER_E4, FEN_AFTER_D4)
        nav.onUserMove(d4fromE4)
        assertEquals(listOf("e4", "d4"), nav.sanList)
    }

    @Test
    fun `goTo with valid id navigates to that node`() {
        val nav = LinearGameNavigator()
        nav.onUserMove(e4())
        val id = nav.currentNodeId!!
        nav.onUserMove(e5())
        assertTrue(nav.goTo(id))
        assertEquals(id, nav.currentNodeId)
        assertEquals(0, nav.currentMoveIndex)
    }

    @Test
    fun `goTo with invalid id returns false`() {
        val nav = LinearGameNavigator()
        assertFalse(nav.goTo(MoveNodeId(999)))
    }

    @Test
    fun `onPositionChanged invoked with initialFen when goPrevious reaches root`() {
        val nav = LinearGameNavigator()
        nav.onUserMove(e4())
        var capturedFen: String? = null
        nav.onPositionChanged = { fen, _ -> capturedFen = fen }
        nav.goPrevious()
        assertEquals(STARTING_FEN, capturedFen)
    }

    @Test
    fun `onPositionChanged invoked with fenAfter on goNext`() {
        val nav = LinearGameNavigator()
        nav.onUserMove(e4())
        nav.goPrevious()
        var capturedFen: String? = null
        nav.onPositionChanged = { fen, _ -> capturedFen = fen }
        nav.goNext()
        assertEquals(FEN_AFTER_E4, capturedFen)
    }

    @Test
    fun `tree updated after onUserMove - rootChildren and nodes populated`() {
        val nav = LinearGameNavigator()
        nav.onUserMove(e4())
        assertEquals(1, nav.tree.rootChildren.size)
        assertEquals(1, nav.tree.nodes.size)
    }

    @Test
    fun `clear resets all state`() {
        val nav = LinearGameNavigator()
        nav.onUserMove(e4())
        nav.clear()
        assertTrue(nav.sanList.isEmpty())
        assertEquals(-1, nav.currentMoveIndex)
        assertTrue(nav.isAtStart)
        assertTrue(nav.isAtEnd)
        assertNull(nav.currentNodeId)
        assertTrue(nav.tree.rootChildren.isEmpty())
    }
}
