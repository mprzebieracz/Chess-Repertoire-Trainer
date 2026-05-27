package com.example.chessrepertoiretrainer.core.network.games

import io.mockk.mockk
import io.mockk.every
import org.junit.Assert.*
import org.junit.Test

class GameFetcherTest {

    // ---- parsePgnHeaders ----

    @Test
    fun `parsePgnHeaders extracts single header`() {
        val pgn = "[Event \"World Championship\"]\n\n1. e4 e5"
        val headers = parsePgnHeaders(pgn)
        assertEquals("World Championship", headers["Event"])
    }

    @Test
    fun `parsePgnHeaders extracts multiple headers`() {
        val pgn = "[Event \"Test\"]\n[White \"Magnus\"]\n[Result \"1-0\"]\n\n1. e4"
        val headers = parsePgnHeaders(pgn)
        assertEquals("Test", headers["Event"])
        assertEquals("Magnus", headers["White"])
        assertEquals("1-0", headers["Result"])
    }

    @Test
    fun `parsePgnHeaders empty string returns empty map`() {
        assertTrue(parsePgnHeaders("").isEmpty())
    }

    @Test
    fun `parsePgnHeaders only movetext returns empty map`() {
        val pgn = "1. e4 e5 2. Nf3 Nc6"
        assertTrue(parsePgnHeaders(pgn).isEmpty())
    }

    @Test
    fun `parsePgnHeaders stops parsing at first non-header line`() {
        val pgn = "[Event \"Test\"]\n1. e4\n[White \"Late\"]"
        val headers = parsePgnHeaders(pgn)
        assertEquals("Test", headers["Event"])
        assertNull(headers["White"])
    }

    @Test
    fun `parsePgnHeaders skips blank lines between headers`() {
        val pgn = "[Event \"Test\"]\n\n[White \"Magnus\"]\n\n1. e4"
        val headers = parsePgnHeaders(pgn)
        assertEquals("Test", headers["Event"])
        assertEquals("Magnus", headers["White"])
    }

    // ---- GameFetcherRegistry ----

    @Test
    fun `getFetcher returns fetcher by exact platform key`() {
        val fetcher = mockk<GameFetcher>()
        every { fetcher.platformKey } returns "lichess"
        val registry = GameFetcherRegistry(listOf(fetcher))
        assertNotNull(registry.getFetcher("lichess"))
    }

    @Test
    fun `getFetcher is case-insensitive`() {
        val fetcher = mockk<GameFetcher>()
        every { fetcher.platformKey } returns "Lichess"
        val registry = GameFetcherRegistry(listOf(fetcher))
        assertNotNull(registry.getFetcher("LICHESS"))
        assertNotNull(registry.getFetcher("lichess"))
    }

    @Test
    fun `getFetcher with leading and trailing spaces`() {
        val fetcher = mockk<GameFetcher>()
        every { fetcher.platformKey } returns "lichess"
        val registry = GameFetcherRegistry(listOf(fetcher))
        assertNotNull(registry.getFetcher("  lichess  "))
    }

    @Test
    fun `getFetcher returns null for unknown platform`() {
        val registry = GameFetcherRegistry(emptyList())
        assertNull(registry.getFetcher("chess.com"))
    }

    @Test
    fun `getFetcher returns correct fetcher when multiple registered`() {
        val fetcherA = mockk<GameFetcher>()
        every { fetcherA.platformKey } returns "lichess"
        val fetcherB = mockk<GameFetcher>()
        every { fetcherB.platformKey } returns "chess.com"
        val registry = GameFetcherRegistry(listOf(fetcherA, fetcherB))
        assertSame(fetcherA, registry.getFetcher("lichess"))
        assertSame(fetcherB, registry.getFetcher("chess.com"))
    }
}
