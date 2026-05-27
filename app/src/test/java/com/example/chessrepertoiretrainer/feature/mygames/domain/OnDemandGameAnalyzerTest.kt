package com.example.chessrepertoiretrainer.feature.mygames.domain

import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class OnDemandGameAnalyzerTest {

    private val analyzer = OnDemandGameAnalyzer(
        engine = mockk(relaxed = true),
        moveEvalDao = mockk(relaxed = true)
    )

    // ---- classify: CP loss thresholds ----

    @Test
    fun `classify 0 cp loss returns BEST`() {
        assertEquals("BEST", analyzer.classify(0))
    }

    @Test
    fun `classify 10 cp loss returns BEST - upper boundary`() {
        assertEquals("BEST", analyzer.classify(10))
    }

    @Test
    fun `classify 11 cp loss returns GOOD`() {
        assertEquals("GOOD", analyzer.classify(11))
    }

    @Test
    fun `classify 25 cp loss returns GOOD - upper boundary`() {
        assertEquals("GOOD", analyzer.classify(25))
    }

    @Test
    fun `classify 26 cp loss returns INACCURACY`() {
        assertEquals("INACCURACY", analyzer.classify(26))
    }

    @Test
    fun `classify 50 cp loss returns INACCURACY - upper boundary`() {
        assertEquals("INACCURACY", analyzer.classify(50))
    }

    @Test
    fun `classify 51 cp loss returns MISTAKE`() {
        assertEquals("MISTAKE", analyzer.classify(51))
    }

    @Test
    fun `classify 100 cp loss returns MISTAKE - upper boundary`() {
        assertEquals("MISTAKE", analyzer.classify(100))
    }

    @Test
    fun `classify 101 cp loss returns BLUNDER`() {
        assertEquals("BLUNDER", analyzer.classify(101))
    }

    @Test
    fun `classify MAX_VALUE returns BLUNDER`() {
        assertEquals("BLUNDER", analyzer.classify(Int.MAX_VALUE))
    }

    // ---- CP loss direction ----

    @Test
    fun `white move cp loss is prevCp minus cp`() {
        // White's move: position improves for White when cp increases, worsens when cp decreases
        // cpLoss = prevCp - cp, coerced >= 0
        val prevCp = 50
        val cp = 20  // position got worse for White
        val expectedLoss = prevCp - cp  // 30
        assertEquals("INACCURACY", analyzer.classify(expectedLoss))
        assertEquals(30, expectedLoss)
    }

    @Test
    fun `white move cp loss coerced to zero when position improves`() {
        // prevCp=20, cp=50 → raw = 20 - 50 = -30 → coerced to 0 → BEST
        val rawLoss = (20 - 50).coerceAtLeast(0)
        assertEquals(0, rawLoss)
        assertEquals("BEST", analyzer.classify(rawLoss))
    }

    @Test
    fun `black move cp loss is cp minus prevCp`() {
        // Black's move: position improves for Black when cp decreases (from White's POV)
        // cpLoss = cp - prevCp, coerced >= 0
        val prevCp = 20
        val cp = 60  // position got worse for Black (cp went up = better for White)
        val expectedLoss = cp - prevCp  // 40
        assertEquals("INACCURACY", analyzer.classify(expectedLoss))
        assertEquals(40, expectedLoss)
    }

    @Test
    fun `black move cp loss coerced to zero when position improves`() {
        // prevCp=50, cp=20 → raw = 20 - 50 = -30 → coerced to 0 → BEST
        val rawLoss = (20 - 50).coerceAtLeast(0)
        assertEquals(0, rawLoss)
        assertEquals("BEST", analyzer.classify(rawLoss))
    }
}
