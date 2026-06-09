package com.example.chessrepertoiretrainer.core.chess.pgn.parse

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PgnLineResolverTest {

    private val resolver = PgnLineResolver(minMovesPerLine = 1)
    private val strictResolver = PgnLineResolver() // default minMovesPerLine = 4

    private fun pm(san: String, comment: String? = null) =
        PgnMovetextParser.ParsedMove(san = san, comment = comment)

    // ---- basic resolution ----

    @Test
    fun `valid single move resolves with fen and san`() {
        val result = resolver.resolveLine(listOf(pm("e4")), null)
        assertEquals(1, result.size)
        assertEquals("e4", result[0].san)
        assertNotNull(result[0].fen)
    }

    @Test
    fun `two moves resolve in order`() {
        val result = resolver.resolveLine(listOf(pm("e4"), pm("e5")), null)
        assertEquals(2, result.size)
        assertEquals("e4", result[0].san)
        assertEquals("e5", result[1].san)
    }

    @Test
    fun `fen after each move is populated`() {
        val result = resolver.resolveLine(listOf(pm("e4"), pm("e5")), null)
        assertTrue(result[0].fen.isNotBlank())
        assertTrue(result[1].fen.isNotBlank())
        assertNotEquals(result[0].fen, result[1].fen)
    }

    @Test
    fun `comment is preserved on resolved move`() {
        val result = resolver.resolveLine(listOf(pm("e4", comment = "Best move")), null)
        assertEquals("Best move", result[0].comment)
    }

    @Test
    fun `null comment is preserved`() {
        val result = resolver.resolveLine(listOf(pm("e4", comment = null)), null)
        assertNull(result[0].comment)
    }

    // ---- FEN tag ----

    @Test
    fun `custom FEN tag sets starting position`() {
        val fen = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1"
        val result = resolver.resolveLine(listOf(pm("e5")), fen)
        assertEquals(1, result.size)
        assertEquals("e5", result[0].san)
    }

    // ---- illegal moves ----

    @Test
    fun `illegal move stops resolution at that point`() {
        // e2e5 is illegal; only e4 resolves
        val result = resolver.resolveLine(listOf(pm("e4"), pm("e4")), null)
        // Second e4 is illegal after first e4 already occupied that square
        assertEquals(1, result.size)
    }

    @Test
    fun `completely illegal first move returns empty`() {
        val result = resolver.resolveLine(listOf(pm("Qe4")), null)
        assertTrue(result.isEmpty())
    }

    // ---- min moves per line ----

    @Test
    fun `line below minMovesPerLine returns empty`() {
        val result = strictResolver.resolveLine(listOf(pm("e4"), pm("e5")), null)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `line meeting minMovesPerLine is returned`() {
        val moves = listOf(pm("e4"), pm("e5"), pm("Nf3"), pm("Nc6"))
        val result = strictResolver.resolveLine(moves, null)
        assertEquals(4, result.size)
    }

    // ---- empty input ----

    @Test
    fun `empty move list returns empty`() {
        val result = resolver.resolveLine(emptyList(), null)
        assertTrue(result.isEmpty())
    }
}