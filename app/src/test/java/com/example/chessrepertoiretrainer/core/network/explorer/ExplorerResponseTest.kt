package com.example.chessrepertoiretrainer.core.network.explorer

import org.junit.Assert.*
import org.junit.Test

class ExplorerResponseTest {

    // ---- ExplorerMove.total ----

    @Test
    fun `ExplorerMove total sums white draws and black`() {
        val move = ExplorerMove(uci = "e2e4", san = "e4", white = 100L, draws = 30L, black = 70L)
        assertEquals(200L, move.total)
    }

    @Test
    fun `ExplorerMove total is zero when all fields are zero`() {
        val move = ExplorerMove(uci = "e2e4", san = "e4", white = 0L, draws = 0L, black = 0L)
        assertEquals(0L, move.total)
    }

    @Test
    fun `ExplorerMove total handles large values`() {
        val move = ExplorerMove(uci = "e2e4", san = "e4", white = 1_000_000L, draws = 500_000L, black = 800_000L)
        assertEquals(2_300_000L, move.total)
    }

    // ---- ExplorerResponse.total ----

    @Test
    fun `ExplorerResponse total sums white draws and black`() {
        val response = ExplorerResponse(
            white = 50L, draws = 20L, black = 30L,
            moves = emptyList(), opening = null
        )
        assertEquals(100L, response.total)
    }

    @Test
    fun `ExplorerResponse total is zero when all fields are zero`() {
        val response = ExplorerResponse(
            white = 0L, draws = 0L, black = 0L,
            moves = emptyList(), opening = null
        )
        assertEquals(0L, response.total)
    }

    @Test
    fun `ExplorerResponse total is independent of moves list`() {
        val response = ExplorerResponse(
            white = 10L, draws = 5L, black = 15L,
            moves = listOf(
                ExplorerMove("e2e4", "e4", white = 10L, draws = 5L, black = 15L)
            ),
            opening = null
        )
        assertEquals(30L, response.total)
    }
}
