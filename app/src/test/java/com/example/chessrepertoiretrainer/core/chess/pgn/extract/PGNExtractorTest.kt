package com.example.chessrepertoiretrainer.core.chess.pgn.extract

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PGNExtractorTest {

    private fun extract(pgn: String) = PGNExtractor.extractSanMovesFromPgn(pgn)

    @Test
    fun `empty string returns empty list`() {
        assertTrue(extract("").isEmpty())
    }

    @Test
    fun `blank string returns empty list`() {
        assertTrue(extract("   \n\n  ").isEmpty())
    }

    @Test
    fun `headers only returns empty list`() {
        val pgn = """
            [Event "Test"]
            [White "Alice"]
            [Black "Bob"]
        """.trimIndent()
        assertTrue(extract(pgn).isEmpty())
    }

    @Test
    fun `standard PGN extracts SAN moves without headers or result`() {
        val pgn = """
            [Event "Test"]

            1. e4 e5 2. Nf3 Nc6 1-0
        """.trimIndent()
        assertEquals(listOf("e4", "e5", "Nf3", "Nc6"), extract(pgn))
    }

    @Test
    fun `game result 1-0 is not included`() {
        val pgn = "1. e4 e5 1-0"
        val moves = extract(pgn)
        assertTrue("1-0" !in moves)
        assertEquals(listOf("e4", "e5"), moves)
    }

    @Test
    fun `game result 0-1 is not included`() {
        val pgn = "1. e4 e5 0-1"
        assertTrue("0-1" !in extract(pgn))
    }

    @Test
    fun `game result 1-2-1-2 draw is not included`() {
        val pgn = "1. e4 e5 1/2-1/2"
        assertTrue("1/2-1/2" !in extract(pgn))
    }

    @Test
    fun `game result asterisk is not included`() {
        val pgn = "1. e4 *"
        assertTrue("*" !in extract(pgn))
        assertEquals(listOf("e4"), extract(pgn))
    }

    @Test
    fun `brace comment is skipped, surrounding moves kept`() {
        val pgn = "1. e4 {This is a comment} e5"
        assertEquals(listOf("e4", "e5"), extract(pgn))
    }

    @Test
    fun `multi-word brace comment spanning whitespace is skipped`() {
        val pgn = "1. e4 {Best move according to theory} e5 2. Nf3"
        assertEquals(listOf("e4", "e5", "Nf3"), extract(pgn))
    }

    @Test
    fun `NAG dollar-number is skipped`() {
        val pgn = "1. e4 ${'$'}1 e5"
        assertEquals(listOf("e4", "e5"), extract(pgn))
    }

    @Test
    fun `inline engine tag is stripped`() {
        val pgn = "1. e4 [%eval +0.50] e5"
        assertEquals(listOf("e4", "e5"), extract(pgn))
    }

    @Test
    fun `inline clock tag is stripped`() {
        val pgn = "1. e4 [%clk 0:05:00] e5"
        assertEquals(listOf("e4", "e5"), extract(pgn))
    }

    @Test
    fun `move numbers are not in output`() {
        val pgn = "1. e4 e5 2. Nf3"
        val moves = extract(pgn)
        assertTrue(moves.none { it.matches(Regex("""^\d+\.+$""")) })
    }

    @Test
    fun `variation parentheses stripped from surrounding moves`() {
        val pgn = "1. e4 (1. d4 d5) e5"
        val moves = extract(pgn)
        assertTrue("e4" in moves)
        assertTrue("d4" in moves)
        assertTrue("d5" in moves)
        assertTrue("e5" in moves)
    }

    @Test
    fun `pgn without headers is parsed from first line`() {
        val pgn = "1. e4 e5 2. Nf3 Nc6"
        assertEquals(listOf("e4", "e5", "Nf3", "Nc6"), extract(pgn))
    }
}
