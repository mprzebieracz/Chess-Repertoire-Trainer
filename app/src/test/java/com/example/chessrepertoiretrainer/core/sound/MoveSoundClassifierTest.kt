package com.example.chessrepertoiretrainer.core.sound

import org.junit.Assert.assertEquals
import org.junit.Test

class MoveSoundClassifierTest {

    @Test
    fun `plain pawn push returns MOVE`() {
        assertEquals(MoveSoundType.MOVE, MoveSoundClassifier.fromSan("e4"))
    }

    @Test
    fun `plain knight move returns MOVE`() {
        assertEquals(MoveSoundType.MOVE, MoveSoundClassifier.fromSan("Nf3"))
    }

    @Test
    fun `capture returns CAPTURE`() {
        assertEquals(MoveSoundType.CAPTURE, MoveSoundClassifier.fromSan("exd5"))
    }

    @Test
    fun `piece capture returns CAPTURE`() {
        assertEquals(MoveSoundType.CAPTURE, MoveSoundClassifier.fromSan("Nxe5"))
    }

    @Test
    fun `check returns CHECK`() {
        assertEquals(MoveSoundType.CHECK, MoveSoundClassifier.fromSan("Qa4+"))
    }

    @Test
    fun `mate returns MATE`() {
        assertEquals(MoveSoundType.MATE, MoveSoundClassifier.fromSan("Qh7#"))
    }

    @Test
    fun `capture with check returns CHECK not CAPTURE`() {
        assertEquals(MoveSoundType.CHECK, MoveSoundClassifier.fromSan("Qxf7+"))
    }

    @Test
    fun `capture with mate returns MATE not CAPTURE`() {
        assertEquals(MoveSoundType.MATE, MoveSoundClassifier.fromSan("Qxf7#"))
    }

    @Test
    fun `MATE takes priority over CHECK in same string`() {
        // Hypothetical - # beats +
        assertEquals(MoveSoundType.MATE, MoveSoundClassifier.fromSan("Rd8#"))
    }

    @Test
    fun `castling move returns MOVE`() {
        assertEquals(MoveSoundType.MOVE, MoveSoundClassifier.fromSan("O-O"))
        assertEquals(MoveSoundType.MOVE, MoveSoundClassifier.fromSan("O-O-O"))
    }
}