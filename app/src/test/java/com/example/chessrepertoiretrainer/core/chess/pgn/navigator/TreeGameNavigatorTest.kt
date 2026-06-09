package com.example.chessrepertoiretrainer.core.chess.pgn.navigator

import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.move.Move
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TreeGameNavigatorTest {

    private val STARTING_FEN = TreeGameNavigator.STARTING_FEN
    private val FEN_AFTER_E4 = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1"
    private val FEN_AFTER_D4 = "rnbqkbnr/pppppppp/8/8/3P4/8/PPP1PPPP/RNBQKBNR b KQkq d3 0 1"
    private val FEN_AFTER_E5 = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR w KQkq e6 0 2"

    private fun e4() = AppliedMove(Move(Square.E2, Square.E4), "e4", STARTING_FEN, FEN_AFTER_E4)
    private fun d4() = AppliedMove(Move(Square.D2, Square.D4), "d4", STARTING_FEN, FEN_AFTER_D4)
    private fun e5() = AppliedMove(Move(Square.E7, Square.E5), "e5", FEN_AFTER_E4, FEN_AFTER_E5)

    @Test
    fun `initial state - isAtStart, isAtEnd, currentMoveIndex is -1`() {
        val nav = TreeGameNavigator()
        assertTrue(nav.isAtStart)
        assertTrue(nav.isAtEnd)
        assertEquals(-1, nav.currentMoveIndex)
        assertNull(nav.currentNodeId)
    }

    @Test
    fun `onUserMove creates new node and returns Accepted`() {
        val nav = TreeGameNavigator()
        val result = nav.onUserMove(e4())
        assertTrue(result is UserMoveResult.Accepted)
        assertNotNull(nav.currentNodeId)
        assertEquals(0, nav.currentMoveIndex)
        assertFalse(nav.isAtStart)
    }

    @Test
    fun `same SAN from same position reuses existing node`() {
        val nav = TreeGameNavigator()
        val first = nav.onUserMove(e4()) as UserMoveResult.Accepted
        nav.goPrevious()
        val second = nav.onUserMove(e4()) as UserMoveResult.Accepted
        assertEquals(first.nodeId, second.nodeId)
        assertEquals(1, nav.tree.rootChildren.size)
    }

    @Test
    fun `different SAN from root creates two root children`() {
        val nav = TreeGameNavigator()
        nav.onUserMove(e4())
        nav.goPrevious()
        nav.onUserMove(d4())
        assertEquals(2, nav.tree.rootChildren.size)
    }

    @Test
    fun `different SAN mid-line creates a variation branch`() {
        val nav = TreeGameNavigator()
        nav.onUserMove(e4())
        val afterE4Id = nav.currentNodeId!!
        nav.onUserMove(e5())
        nav.goTo(afterE4Id)
        // Play something other than e5 — creates a sibling of e5
        val c5 = AppliedMove(Move(Square.C7, Square.C5), "c5", FEN_AFTER_E4, FEN_AFTER_D4)
        nav.onUserMove(c5)
        val e4Node = nav.tree.nodes[afterE4Id]!!
        assertEquals(2, e4Node.children.size)
    }

    @Test
    fun `goPrevious returns to parent node`() {
        val nav = TreeGameNavigator()
        nav.onUserMove(e4())
        nav.onUserMove(e5())
        assertEquals(1, nav.currentMoveIndex)
        assertTrue(nav.goPrevious())
        assertEquals(0, nav.currentMoveIndex)
        assertTrue(nav.goPrevious())
        assertEquals(-1, nav.currentMoveIndex)
        assertTrue(nav.isAtStart)
    }

    @Test
    fun `goPrevious at root returns false`() {
        val nav = TreeGameNavigator()
        assertFalse(nav.goPrevious())
    }

    @Test
    fun `goNext from root follows first root child`() {
        val nav = TreeGameNavigator()
        nav.onUserMove(e4())
        nav.goPrevious()
        assertTrue(nav.goNext())
        assertEquals(0, nav.currentMoveIndex)
    }

    @Test
    fun `goNext at leaf returns false`() {
        val nav = TreeGameNavigator()
        nav.onUserMove(e4())
        assertFalse(nav.goNext())
    }

    @Test
    fun `currentMoveIndex reflects depth - root child is 0`() {
        val nav = TreeGameNavigator()
        nav.onUserMove(e4())
        assertEquals(0, nav.currentMoveIndex)
    }

    @Test
    fun `currentMoveIndex reflects depth - child of root child is 1`() {
        val nav = TreeGameNavigator()
        nav.onUserMove(e4())
        nav.onUserMove(e5())
        assertEquals(1, nav.currentMoveIndex)
    }

    @Test
    fun `goTo with valid id navigates to that node`() {
        val nav = TreeGameNavigator()
        nav.onUserMove(e4())
        val e4Id = nav.currentNodeId!!
        nav.onUserMove(e5())
        assertTrue(nav.goTo(e4Id))
        assertEquals(e4Id, nav.currentNodeId)
        assertEquals(0, nav.currentMoveIndex)
    }

    @Test
    fun `goTo with invalid id returns false`() {
        val nav = TreeGameNavigator()
        assertFalse(nav.goTo(MoveNodeId(999)))
    }

    @Test
    fun `isAtEnd false when current node has children`() {
        val nav = TreeGameNavigator()
        nav.onUserMove(e4())
        val e4Id = nav.currentNodeId!!
        nav.onUserMove(e5())
        nav.goTo(e4Id)
        assertFalse(nav.isAtEnd)
    }

    @Test
    fun `isAtEnd true when current node has no children`() {
        val nav = TreeGameNavigator()
        nav.onUserMove(e4())
        assertTrue(nav.isAtEnd)
    }

    @Test
    fun `onPositionChanged invoked on goPrevious`() {
        val nav = TreeGameNavigator()
        nav.onUserMove(e4())
        var capturedFen: String? = null
        nav.onPositionChanged = { fen, _ -> capturedFen = fen }
        nav.goPrevious()
        assertEquals(STARTING_FEN, capturedFen)
    }

    @Test
    fun `onPositionChanged invoked on goNext`() {
        val nav = TreeGameNavigator()
        nav.onUserMove(e4())
        nav.goPrevious()
        var capturedFen: String? = null
        nav.onPositionChanged = { fen, _ -> capturedFen = fen }
        nav.goNext()
        assertEquals(FEN_AFTER_E4, capturedFen)
    }

    @Test
    fun `reset goes to initial position and invokes callback`() {
        val nav = TreeGameNavigator()
        nav.onUserMove(e4())
        var capturedFen: String? = null
        nav.onPositionChanged = { fen, _ -> capturedFen = fen }
        nav.reset()
        assertTrue(nav.isAtStart)
        assertEquals(-1, nav.currentMoveIndex)
        assertEquals(STARTING_FEN, capturedFen)
    }

    @Test
    fun `clear resets all state`() {
        val nav = TreeGameNavigator()
        nav.onUserMove(e4())
        nav.clear()
        assertTrue(nav.isAtStart)
        assertTrue(nav.isAtEnd)
        assertEquals(-1, nav.currentMoveIndex)
        assertNull(nav.currentNodeId)
        assertTrue(nav.tree.rootChildren.isEmpty())
    }
}