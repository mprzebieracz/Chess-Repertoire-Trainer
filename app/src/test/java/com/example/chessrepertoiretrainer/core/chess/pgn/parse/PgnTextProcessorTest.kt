package com.example.chessrepertoiretrainer.core.chess.pgn.parse

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PgnTextProcessorTest {

    private val processor = PgnTextProcessor()

    // ---- preprocess ----

    @Test
    fun `preprocess converts Windows line endings to Unix`() {
        val input = "line1\r\nline2\r\nline3"
        val result = processor.preprocess(input)
        assertTrue('\r' !in result)
        assertEquals(3, result.lines().size)
    }

    @Test
    fun `preprocess converts old Mac line endings to Unix`() {
        val input = "line1\rline2\rline3"
        val result = processor.preprocess(input)
        assertTrue('\r' !in result)
        assertEquals(3, result.lines().size)
    }

    @Test
    fun `preprocess replaces non-breaking space with regular space`() {
        val nbsp = ' '
        val input = "1.${nbsp}e4"
        val result = processor.preprocess(input)
        assertTrue(nbsp !in result)
        assertTrue(' ' in result)
    }

    @Test
    fun `preprocess removes inline engine eval tag`() {
        val input = "1. e4 [%eval +0.50] e5"
        val result = processor.preprocess(input)
        assertTrue("[%eval" !in result)
        assertTrue("e4" in result)
        assertTrue("e5" in result)
    }

    @Test
    fun `preprocess removes inline clock tag`() {
        val input = "1. e4 [%clk 0:05:00] e5"
        val result = processor.preprocess(input)
        assertTrue("[%clk" !in result)
    }

    @Test
    fun `preprocess trims trailing whitespace from each line`() {
        val input = "line1   \nline2\t\nline3"
        val lines = processor.preprocess(input).lines()
        lines.forEach { line -> assertEquals(line, line.trimEnd()) }
    }

    @Test
    fun `preprocess preserves leading whitespace within line`() {
        val input = "  1. e4 e5"
        val result = processor.preprocess(input)
        assertTrue(result.startsWith("  "))
    }

    @Test
    fun `preprocess leaves normal PGN unchanged except normalization`() {
        val input = "1. e4 e5 2. Nf3 Nc6"
        val result = processor.preprocess(input)
        assertEquals(input, result)
    }

    // ---- splitIntoGames ----

    @Test
    fun `splitIntoGames on empty string returns empty list`() {
        assertTrue(processor.splitIntoGames("").isEmpty())
    }

    @Test
    fun `splitIntoGames single game returns one element`() {
        val pgn = "[Event \"Test\"]\n\n1. e4 e5 1-0"
        val games = processor.splitIntoGames(pgn)
        assertEquals(1, games.size)
        assertTrue(games[0].contains("e4"))
    }

    @Test
    fun `splitIntoGames two games returns two elements`() {
        // Each [header] line starts a new game; use one header per game
        val pgn = "[Event \"Game1\"]\n\n1. e4 e5 1-0\n\n[Event \"Game2\"]\n\n1. d4 d5 0-1"
        val games = processor.splitIntoGames(pgn)
        assertEquals(2, games.size)
        assertTrue(games[0].contains("e4"))
        assertTrue(games[1].contains("d4"))
    }

    @Test
    fun `splitIntoGames header line starts a new game`() {
        val pgn = "[Event \"A\"]\n1. e4\n[Event \"B\"]\n1. d4"
        val games = processor.splitIntoGames(pgn)
        assertEquals(2, games.size)
    }

    @Test
    fun `splitIntoGames preserves moves in each game`() {
        val pgn = "[Event \"Test\"]\n\n1. e4 e5 2. Nf3 1-0"
        val games = processor.splitIntoGames(pgn)
        assertTrue(games[0].contains("Nf3"))
    }
}
