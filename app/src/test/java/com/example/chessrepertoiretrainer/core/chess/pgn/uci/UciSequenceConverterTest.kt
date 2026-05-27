package com.example.chessrepertoiretrainer.core.chess.pgn.uci

import org.junit.Assert.*
import org.junit.Test

class UciSequenceConverterTest {

    private val STARTING_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

    @Test
    fun `empty token list returns empty list`() {
        assertTrue(convertUciSequenceToSan(STARTING_FEN, emptyList()).isEmpty())
    }

    @Test
    fun `single pawn push converted to SAN`() {
        val result = convertUciSequenceToSan(STARTING_FEN, listOf("e2e4"))
        assertEquals(listOf("e4"), result)
    }

    @Test
    fun `knight move converted to SAN`() {
        val result = convertUciSequenceToSan(STARTING_FEN, listOf("g1f3"))
        assertEquals(listOf("Nf3"), result)
    }

    @Test
    fun `sequence of two moves converted correctly`() {
        val result = convertUciSequenceToSan(STARTING_FEN, listOf("e2e4", "e7e5"))
        assertEquals(listOf("e4", "e5"), result)
    }

    @Test
    fun `three move sequence including Nf3`() {
        val result = convertUciSequenceToSan(STARTING_FEN, listOf("e2e4", "e7e5", "g1f3"))
        assertEquals(listOf("e4", "e5", "Nf3"), result)
    }

    @Test
    fun `illegal move returns empty list`() {
        // e2e5 is not a legal pawn move
        val result = convertUciSequenceToSan(STARTING_FEN, listOf("e2e5"))
        assertTrue(result.isEmpty())
    }

    @Test
    fun `valid moves followed by illegal move returns empty list`() {
        val result = convertUciSequenceToSan(STARTING_FEN, listOf("e2e4", "e7e6", "e1e3"))
        assertTrue(result.isEmpty())
    }

    @Test
    fun `promotion move converted with promotion piece`() {
        // Position with white pawn on e7 about to promote
        val fen = "8/4P3/8/8/8/8/8/4K1k1 w - - 0 1"
        val result = convertUciSequenceToSan(fen, listOf("e7e8q"))
        assertEquals(1, result.size)
        assertTrue(result[0].contains("Q") || result[0].contains("=Q"))
    }

    @Test
    fun `castling move converted to O-O`() {
        // Position where white can castle kingside
        val fen = "r3k2r/pppppppp/8/8/8/8/PPPPPPPP/R3K2R w KQkq - 0 1"
        val result = convertUciSequenceToSan(fen, listOf("e1g1"))
        assertEquals(listOf("O-O"), result)
    }

    @Test
    fun `invalid FEN returns empty list`() {
        val result = convertUciSequenceToSan("not a fen", listOf("e2e4"))
        assertTrue(result.isEmpty())
    }

    @Test
    fun `malformed UCI token returns empty list`() {
        val result = convertUciSequenceToSan(STARTING_FEN, listOf("abc"))
        assertTrue(result.isEmpty())
    }
}
