package com.example.chessrepertoiretrainer.core.chess.pgn.parse

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PgnMovetextParserTest {

    private val parser = PgnMovetextParser()

    private fun parse(text: String) = parser.parseLinesFromMovetext(text)

    @Test
    fun `empty string returns empty list`() {
        assertTrue(parse("").isEmpty())
    }

    @Test
    fun `simple linear moves returns one line`() {
        val lines = parse("1. e4 e5 2. Nf3")
        assertEquals(1, lines.size)
        val moves = lines[0].map { it.san }
        assertEquals(listOf("e4", "e5", "Nf3"), moves)
    }

    @Test
    fun `game result 1-0 terminates parsing`() {
        val lines = parse("1. e4 e5 1-0 2. Nf3")
        assertEquals(1, lines.size)
        assertEquals(listOf("e4", "e5"), lines[0].map { it.san })
    }

    @Test
    fun `game result 0-1 terminates parsing`() {
        val lines = parse("1. e4 e5 0-1")
        assertEquals(listOf("e4", "e5"), lines[0].map { it.san })
    }

    @Test
    fun `game result asterisk terminates parsing`() {
        val lines = parse("1. e4 *")
        assertEquals(listOf("e4"), lines[0].map { it.san })
    }

    @Test
    fun `brace comment attached to preceding move`() {
        val lines = parse("1. e4 {Best move} e5")
        assertEquals(1, lines.size)
        val e4 = lines[0].first { it.san == "e4" }
        assertEquals("Best move", e4.comment)
    }

    @Test
    fun `brace comment before first move stored as pending for next move`() {
        val lines = parse("{Opening} 1. e4 e5")
        assertEquals(1, lines.size)
        val e4 = lines[0].first { it.san == "e4" }
        assertEquals("Opening", e4.comment)
    }

    @Test
    fun `move with no comment has null comment`() {
        val lines = parse("1. e4 e5")
        assertNull(lines[0][0].comment)
    }

    @Test
    fun `NAG dollar-number is skipped`() {
        val lines = parse("1. e4 ${'$'}1 e5")
        assertEquals(listOf("e4", "e5"), lines[0].map { it.san })
    }

    @Test
    fun `variation produces additional line`() {
        val lines = parse("1. e4 e5 (1... c5) 2. Nf3")
        assertEquals(2, lines.size)
        val mainSans = lines[0].map { it.san }
        assertEquals(listOf("e4", "e5", "Nf3"), mainSans)
        val varSans = lines[1].map { it.san }
        assertTrue("c5" in varSans)
    }

    @Test
    fun `variation inherits parent prefix excluding last move`() {
        val lines = parse("1. e4 (1. d4 d5) e5")
        val varLine = lines.first { it.any { m -> m.san == "d4" } }
        assertEquals(listOf("d4", "d5"), varLine.map { it.san })
    }

    @Test
    fun `nested variation produces its own line`() {
        val lines = parse("1. e4 (1. d4 (1. c4 c5) d5)")
        assertTrue(lines.any { it.any { m -> m.san == "c5" } })
    }

    @Test
    fun `multiple variations produce multiple lines`() {
        val lines = parse("1. e4 e5 (1... c5) (1... e6)")
        assertTrue(lines.size >= 3)
    }

    @Test
    fun `inline square-bracket tag is skipped`() {
        val lines = parse("1. e4 [%eval +0.5] e5")
        assertEquals(listOf("e4", "e5"), lines[0].map { it.san })
    }

    @Test
    fun `move numbers with multiple dots are skipped`() {
        val lines = parse("1. e4 1... e5")
        assertEquals(listOf("e4", "e5"), lines[0].map { it.san })
    }
}
