package com.example.chessrepertoiretrainer.core.opening

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class OpeningClassifierTest {

    private val e4Entry = OpeningEntry("C20", "King's Pawn Game", "normalized_fen_e4", "King's Pawn Game")
    private val sicilianEntry = OpeningEntry("B20", "Sicilian Defense", "normalized_fen_c5", "Sicilian Defense")

    // ---- normalizeFen ----

    @Test
    fun `normalizeFen keeps first 4 space-separated fields`() {
        val fen = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1"
        val normalized = OpeningClassifier.normalizeFen(fen)
        assertEquals("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3", normalized)
    }

    @Test
    fun `normalizeFen strips halfmove and fullmove counters`() {
        val fen = "8/8/8/8/8/8/8/8 w - - 0 1"
        val result = OpeningClassifier.normalizeFen(fen)
        assertEquals("8/8/8/8/8/8/8/8 w - -", result)
    }

    @Test
    fun `normalizeFen with no en passant dash still strips counters`() {
        val fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
        val result = OpeningClassifier.normalizeFen(fen)
        assertEquals("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq -", result)
    }

    @Test
    fun `normalizeFen with starting FEN`() {
        val fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
        val result = OpeningClassifier.normalizeFen(fen)
        assertTrue4Fields(result)
    }

    // ---- classify ----

    @Test
    fun `classify returns entry for matching ECO header`() {
        val registry = mockk<OpeningRegistry>()
        every { registry.lookupByEco("B20") } returns sicilianEntry
        val pgn = """
            [Event "Test"]
            [ECO "B20"]

            1. e4 c5
        """.trimIndent()
        val result = OpeningClassifier.classify(pgn, registry)
        assertNotNull(result)
        assertEquals("Sicilian Defense", result!!.name)
    }

    @Test
    fun `classify returns null when no ECO header`() {
        val registry = mockk<OpeningRegistry>()
        val pgn = "[Event \"Test\"]\n\n1. e4 e5"
        assertNull(OpeningClassifier.classify(pgn, registry))
    }

    @Test
    fun `classify returns null when ECO header present but not in registry`() {
        val registry = mockk<OpeningRegistry>()
        every { registry.lookupByEco("Z99") } returns null
        val pgn = "[ECO \"Z99\"]\n\n1. e4 e5"
        assertNull(OpeningClassifier.classify(pgn, registry))
    }

    // ---- classifyByFenHistory ----

    @Test
    fun `classifyByFenHistory returns null for empty list`() {
        val registry = mockk<OpeningRegistry>()
        assertNull(OpeningClassifier.classifyByFenHistory(emptyList(), registry))
    }

    @Test
    fun `classifyByFenHistory returns matching entry for single FEN`() {
        val registry = mockk<OpeningRegistry>()
        val normalizedFen = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3"
        every { registry.lookupByFen(normalizedFen) } returns e4Entry
        val fullFen = "$normalizedFen 0 1"
        val result = OpeningClassifier.classifyByFenHistory(listOf(fullFen), registry)
        assertEquals(e4Entry, result)
    }

    @Test
    fun `classifyByFenHistory returns null when no FEN matches`() {
        val registry = mockk<OpeningRegistry>()
        every { registry.lookupByFen(any()) } returns null
        val result = OpeningClassifier.classifyByFenHistory(listOf("some fen here - 0 1"), registry)
        assertNull(result)
    }

    @Test
    fun `classifyByFenHistory searches in reverse order deepest first`() {
        val registry = mockk<OpeningRegistry>()
        val fen1 = "fen1 w - - 0 1"
        val fen2 = "fen2 b - - 0 2"
        val fen3 = "fen3 w - - 0 3"
        every { registry.lookupByFen("fen1 w - -") } returns null
        every { registry.lookupByFen("fen2 b - -") } returns null
        every { registry.lookupByFen("fen3 w - -") } returns sicilianEntry

        val result = OpeningClassifier.classifyByFenHistory(listOf(fen1, fen2, fen3), registry)
        assertEquals(sicilianEntry, result)
        verify(exactly = 1) { registry.lookupByFen("fen3 w - -") }
    }

    @Test
    fun `classifyByFenHistory returns deepest match when multiple FENs match`() {
        val registry = mockk<OpeningRegistry>()
        val fen1 = "fen1 w - - 0 1"
        val fen2 = "fen2 b - - 0 2"
        every { registry.lookupByFen("fen1 w - -") } returns e4Entry
        every { registry.lookupByFen("fen2 b - -") } returns sicilianEntry

        val result = OpeningClassifier.classifyByFenHistory(listOf(fen1, fen2), registry)
        assertEquals(sicilianEntry, result)
    }

    private fun assertTrue4Fields(fen: String) {
        assertEquals(4, fen.split(" ").size)
    }
}
