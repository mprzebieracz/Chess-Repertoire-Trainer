package com.example.chessrepertoiretrainer.core.chess.domain

import com.github.bhlangonijr.chesslib.Square
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArrowTest {

    // ---- serializeArrows ----

    @Test
    fun `serializeArrows - empty list produces empty string`() {
        assertEquals("", emptyList<Arrow>().serializeArrows())
    }

    @Test
    fun `serializeArrows - single arrow`() {
        val arrows = listOf(Arrow(Square.A1, Square.B2))
        assertEquals("a1b2", arrows.serializeArrows())
    }

    @Test
    fun `serializeArrows - two arrows joined by comma`() {
        val arrows = listOf(Arrow(Square.A1, Square.B2), Arrow(Square.C3, Square.D4))
        assertEquals("a1b2,c3d4", arrows.serializeArrows())
    }

    @Test
    fun `serializeArrows - highlight arrow (from == to)`() {
        val arrows = listOf(Arrow(Square.E4, Square.E4))
        assertEquals("e4e4", arrows.serializeArrows())
    }

    // ---- parseArrows ----

    @Test
    fun `parseArrows - null input returns empty list`() {
        val result: String? = null
        assertEquals(emptyList<Arrow>(), result.parseArrows())
    }

    @Test
    fun `parseArrows - empty string returns empty list`() {
        assertEquals(emptyList<Arrow>(), "".parseArrows())
    }

    @Test
    fun `parseArrows - valid 4-char token`() {
        val result = "a1b2".parseArrows()
        assertEquals(1, result.size)
        assertEquals(Square.A1, result[0].from)
        assertEquals(Square.B2, result[0].to)
    }

    @Test
    fun `parseArrows - two valid tokens`() {
        val result = "a1b2,c3d4".parseArrows()
        assertEquals(2, result.size)
        assertEquals(Square.C3, result[1].from)
        assertEquals(Square.D4, result[1].to)
    }

    @Test
    fun `parseArrows - token too short is skipped`() {
        val result = "ab".parseArrows()
        assertEquals(emptyList<Arrow>(), result)
    }

    @Test
    fun `parseArrows - token too long is skipped`() {
        val result = "a1b2c".parseArrows()
        assertEquals(emptyList<Arrow>(), result)
    }

    @Test
    fun `parseArrows - mixed valid and invalid tokens keeps valid`() {
        val result = "a1b2,ab,c3d4".parseArrows()
        assertEquals(2, result.size)
        assertEquals(Square.A1, result[0].from)
        assertEquals(Square.C3, result[1].from)
    }

    @Test
    fun `parseArrows - invalid square name is skipped`() {
        val result = "z9z9".parseArrows()
        assertEquals(emptyList<Arrow>(), result)
    }

    @Test
    fun `parseArrows - round-trip with serializeArrows`() {
        val original = listOf(Arrow(Square.E2, Square.E4), Arrow(Square.D7, Square.D5))
        val serialized = original.serializeArrows()
        val parsed = serialized.parseArrows()
        assertEquals(original.size, parsed.size)
        assertEquals(original[0].from, parsed[0].from)
        assertEquals(original[0].to, parsed[0].to)
        assertEquals(original[1].from, parsed[1].from)
        assertEquals(original[1].to, parsed[1].to)
    }

    // ---- toggle ----

    @Test
    fun `toggle - adding arrow to empty list`() {
        val arrow = Arrow(Square.A1, Square.B2)
        val result = emptyList<Arrow>().toggle(arrow)
        assertEquals(listOf(arrow), result)
    }

    @Test
    fun `toggle - adding arrow not already present`() {
        val existing = Arrow(Square.A1, Square.B2)
        val newArrow = Arrow(Square.C3, Square.D4)
        val result = listOf(existing).toggle(newArrow)
        assertEquals(2, result.size)
        assertTrue(result.contains(existing))
        assertTrue(result.contains(newArrow))
    }

    @Test
    fun `toggle - removing arrow that is present`() {
        val arrow = Arrow(Square.A1, Square.B2)
        val result = listOf(arrow).toggle(arrow)
        assertEquals(emptyList<Arrow>(), result)
    }

    @Test
    fun `toggle - removing one arrow leaves others`() {
        val a1 = Arrow(Square.A1, Square.B2)
        val a2 = Arrow(Square.C3, Square.D4)
        val result = listOf(a1, a2).toggle(a1)
        assertEquals(listOf(a2), result)
    }

    // ---- Arrow.isHighlight ----

    @Test
    fun `isHighlight - true when from equals to`() {
        assertTrue(Arrow(Square.E4, Square.E4).isHighlight)
    }

    @Test
    fun `isHighlight - false when from differs from to`() {
        assertFalse(Arrow(Square.E2, Square.E4).isHighlight)
    }
}
