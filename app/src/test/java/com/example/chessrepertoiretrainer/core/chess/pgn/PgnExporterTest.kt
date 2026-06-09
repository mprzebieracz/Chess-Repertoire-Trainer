package com.example.chessrepertoiretrainer.core.chess.pgn

import com.example.chessrepertoiretrainer.core.database.entity.LineMove
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PgnExporterTest {

    private fun lm(san: String, index: Int, comment: String? = null) =
        LineMove(
            id = 0,
            lineId = 1,
            moveIndex = index,
            moveSan = san,
            fen = "",
            comment = comment,
            arrows = null
        )

    // ---- headers ----

    @Test
    fun `empty headers produce default seven-tag roster`() {
        val pgn = PgnExporter.export(emptyList())
        assertTrue(pgn.contains("[Event \"?\"]"))
        assertTrue(pgn.contains("[White \"?\"]"))
        assertTrue(pgn.contains("[Result \"*\"]"))
    }

    @Test
    fun `custom headers are used instead of defaults`() {
        val pgn = PgnExporter.export(
            emptyList(),
            mapOf("Event" to "World Championship", "Result" to "1-0")
        )
        assertTrue(pgn.contains("[Event \"World Championship\"]"))
        assertTrue(pgn.contains("[Result \"1-0\"]"))
        assertFalse(pgn.contains("[White \"?\"]"))
    }

    // ---- move numbering ----

    @Test
    fun `white moves get a number prefix`() {
        val pgn = PgnExporter.export(listOf(lm("e4", 0)))
        assertTrue(pgn.contains("1. e4"))
    }

    @Test
    fun `black move does not get a number prefix`() {
        val pgn = PgnExporter.export(listOf(lm("e4", 0), lm("e5", 1)))
        assertTrue(pgn.contains("1. e4 e5"))
        assertFalse(pgn.contains("1. e5"))
    }

    @Test
    fun `second white move gets number 2`() {
        val moves = listOf(lm("e4", 0), lm("e5", 1), lm("Nf3", 2))
        val pgn = PgnExporter.export(moves)
        assertTrue(pgn.contains("2. Nf3"))
    }

    @Test
    fun `four moves produce correct numbering`() {
        val moves = listOf(lm("e4", 0), lm("e5", 1), lm("Nf3", 2), lm("Nc6", 3))
        val pgn = PgnExporter.export(moves)
        assertTrue(pgn.contains("1. e4 e5"))
        assertTrue(pgn.contains("2. Nf3 Nc6"))
    }

    // ---- comments ----

    @Test
    fun `move with comment includes brace comment`() {
        val pgn = PgnExporter.export(listOf(lm("e4", 0, comment = "Best move")))
        assertTrue(pgn.contains("{Best move}"))
    }

    @Test
    fun `move without comment has no brace`() {
        val pgn = PgnExporter.export(listOf(lm("e4", 0, comment = null)))
        assertFalse(pgn.contains("{"))
    }

    @Test
    fun `blank comment is not included`() {
        val pgn = PgnExporter.export(listOf(lm("e4", 0, comment = "  ")))
        assertFalse(pgn.contains("{"))
    }

    // ---- game result ----

    @Test
    fun `result from headers appended at end`() {
        val pgn = PgnExporter.export(listOf(lm("e4", 0)), mapOf("Result" to "1-0"))
        assertTrue(pgn.trimEnd().endsWith("1-0"))
    }

    @Test
    fun `no headers uses asterisk as result`() {
        val pgn = PgnExporter.export(listOf(lm("e4", 0)))
        assertTrue(pgn.trimEnd().endsWith("*"))
    }

    // ---- empty moves ----

    @Test
    fun `empty move list produces only headers and result`() {
        val pgn = PgnExporter.export(emptyList())
        assertFalse(pgn.contains("1."))
        assertTrue(pgn.trimEnd().endsWith("*"))
    }
}