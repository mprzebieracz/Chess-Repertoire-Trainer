package com.example.chessrepertoiretrainer.core.chess.domain

import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.Side
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.move.Move
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    // ---- toSan ----

    @Test
    fun `toSan - pawn push`() {
        val board = Board()
        val move = Move(Square.E2, Square.E4)
        assertEquals("e4", board.toSan(move))
    }

    @Test
    fun `toSan - knight move`() {
        val board = Board()
        val move = Move(Square.G1, Square.F3)
        assertEquals("Nf3", board.toSan(move))
    }

    @Test
    fun `toSan - pawn capture includes from-file`() {
        val board = Board()
        board.loadFromFen("rnbqkbnr/ppp1pppp/8/3p4/4P3/8/PPPP1PPP/RNBQKBNR w KQkq d6 0 2")
        val move = Move(Square.E4, Square.D5)
        assertEquals("exd5", board.toSan(move))
    }

    @Test
    fun `toSan - kingside castling`() {
        val board = Board()
        board.loadFromFen("r3k2r/pppppppp/8/8/8/8/PPPPPPPP/R3K2R w KQkq - 0 1")
        val move = Move(Square.E1, Square.G1)
        assertEquals("O-O", board.toSan(move))
    }

    @Test
    fun `toSan - queenside castling`() {
        val board = Board()
        board.loadFromFen("r3k2r/pppppppp/8/8/8/8/PPPPPPPP/R3K2R w KQkq - 0 1")
        val move = Move(Square.E1, Square.C1)
        assertEquals("O-O-O", board.toSan(move))
    }

    @Test
    fun `toSan - promotion adds equals sign`() {
        val board = Board()
        board.loadFromFen("8/4P3/8/8/8/8/8/4K1k1 w - - 0 1")
        val move = Move(Square.E7, Square.E8, Piece.WHITE_QUEEN)
        val san = board.toSan(move)
        assertTrue(san.contains("=Q") || san.contains("Q"))
        assertTrue(san.startsWith("e"))
    }

    @Test
    fun `toSan - check move appends plus`() {
        val board = Board()
        board.loadFromFen("4k3/8/8/8/8/8/8/4KR2 w - - 0 1")
        val move = board.moveFromSan("Rf8")!!
        val san = board.toSan(move)
        assertTrue(san.endsWith("+"))
    }

    @Test
    fun `toSan - disambiguation by file when two rooks can reach same square`() {
        // Rooks on a3 and h3 with nothing blocking — both can go to d3
        val board = Board()
        board.loadFromFen("4k3/8/8/8/8/R6R/8/4K3 w - - 0 1")
        val legalSans = board.legalMoves().map { board.toSan(it) }
        assertTrue(legalSans.any { it == "Rad3" })
        assertTrue(legalSans.any { it == "Rhd3" })
    }

    // ---- moveFromSan ----

    @Test
    fun `moveFromSan - valid san returns move`() {
        val board = Board()
        val move = board.moveFromSan("e4")
        assertNotNull(move)
        assertEquals(Square.E2, move!!.from)
        assertEquals(Square.E4, move.to)
    }

    @Test
    fun `moveFromSan - san with check suffix still matches`() {
        val board = Board()
        board.loadFromFen("4k3/8/8/8/8/8/8/4KR2 w - - 0 1")
        val move = board.moveFromSan("Rf8+")
        assertNotNull(move)
    }

    @Test
    fun `moveFromSan - invalid san returns null`() {
        val board = Board()
        assertNull(board.moveFromSan("Qe4"))
    }
}
