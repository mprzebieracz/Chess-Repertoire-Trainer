package com.example.chessrepertoiretrainer.core.chess.domain

import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.Side
import com.github.bhlangonijr.chesslib.Square
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ChessUtilsTest {

    // ---- toSide ----

    @Test
    fun `toSide - White returns WHITE`() {
        assertEquals(Side.WHITE, "White".toSide())
    }

    @Test
    fun `toSide - white lowercase returns WHITE`() {
        assertEquals(Side.WHITE, "white".toSide())
    }

    @Test
    fun `toSide - BLACK uppercase returns BLACK`() {
        assertEquals(Side.BLACK, "BLACK".toSide())
    }

    @Test
    fun `toSide - Black mixed case returns BLACK`() {
        assertEquals(Side.BLACK, "Black".toSide())
    }

    @Test
    fun `toSide - unknown string defaults to BLACK`() {
        assertEquals(Side.BLACK, "unknown".toSide())
    }

    // ---- uciToMove ----

    @Test
    fun `uciToMove - valid 4-char pawn move`() {
        val board = Board()
        val move = uciToMove("e2e4", board)
        assertNotNull(move)
        assertEquals(Square.E2, move!!.from)
        assertEquals(Square.E4, move.to)
        assertEquals(Piece.NONE, move.promotion)
    }

    @Test
    fun `uciToMove - valid knight move`() {
        val board = Board()
        val move = uciToMove("g1f3", board)
        assertNotNull(move)
        assertEquals(Square.G1, move!!.from)
        assertEquals(Square.F3, move.to)
    }

    @Test
    fun `uciToMove - promotion to queen`() {
        val board = Board()
        board.loadFromFen("8/P7/8/8/8/8/8/K1k5 w - - 0 1")
        val move = uciToMove("a7a8q", board)
        assertNotNull(move)
        assertEquals(Square.A7, move!!.from)
        assertEquals(Square.A8, move.to)
        assertEquals(Piece.WHITE_QUEEN, move.promotion)
    }

    @Test
    fun `uciToMove - promotion to rook`() {
        val board = Board()
        board.loadFromFen("8/P7/8/8/8/8/8/K1k5 w - - 0 1")
        val move = uciToMove("a7a8r", board)
        assertNotNull(move)
        assertEquals(Piece.WHITE_ROOK, move!!.promotion)
    }

    @Test
    fun `uciToMove - promotion to bishop`() {
        val board = Board()
        board.loadFromFen("8/P7/8/8/8/8/8/K1k5 w - - 0 1")
        val move = uciToMove("a7a8b", board)
        assertNotNull(move)
        assertEquals(Piece.WHITE_BISHOP, move!!.promotion)
    }

    @Test
    fun `uciToMove - promotion to knight`() {
        val board = Board()
        board.loadFromFen("8/P7/8/8/8/8/8/K1k5 w - - 0 1")
        val move = uciToMove("a7a8n", board)
        assertNotNull(move)
        assertEquals(Piece.WHITE_KNIGHT, move!!.promotion)
    }

    @Test
    fun `uciToMove - too short returns null`() {
        val board = Board()
        assertNull(uciToMove("abc", board))
    }

    @Test
    fun `uciToMove - too long non-promotion returns null`() {
        val board = Board()
        assertNull(uciToMove("e2e4x", board))
    }

    @Test
    fun `uciToMove - empty string returns null`() {
        val board = Board()
        assertNull(uciToMove("", board))
    }

    @Test
    fun `uciToMove - invalid square name returns null`() {
        val board = Board()
        assertNull(uciToMove("z9z9", board))
    }

    @Test
    fun `uciToMove - black promotion uses black piece`() {
        val board = Board()
        board.loadFromFen("K1k5/8/8/8/8/8/p7/8 b - - 0 1")
        val move = uciToMove("a2a1q", board)
        assertNotNull(move)
        assertEquals(Piece.BLACK_QUEEN, move!!.promotion)
    }
}
