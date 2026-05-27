package com.example.chessrepertoiretrainer.core.network.games

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class ChessComGameFetcherTest {

    // ---- helpers ----

    private val SAMPLE_PGN =
        "[Event \"Live Chess\"]\n[White \"playerA\"]\n[Black \"playerB\"]\n" +
        "[Result \"1-0\"]\n[TimeControl \"600\"]\n[Opening \"Italian Game\"]\n\n1. e4 e5 1-0"

    private fun gameJson(
        uuid: String? = "uuid-abc-123",
        url: String? = "https://chess.com/game/123",
        pgn: String = SAMPLE_PGN,
        whiteUser: String = "playerA",
        blackUser: String = "playerB",
        whiteRating: Int = 1500,
        blackRating: Int = 1600,
        endTimeSec: Long = 1_700_000_000L,
        rated: Boolean = true,
        timeClass: String = "rapid",
        timeControl: String = "600"
    ): JSONObject {
        val obj = JSONObject()
        if (uuid != null) obj.put("uuid", uuid)
        if (url != null) obj.put("url", url)
        obj.put("pgn", pgn)
        obj.put("white", JSONObject().put("username", whiteUser).put("rating", whiteRating))
        obj.put("black", JSONObject().put("username", blackUser).put("rating", blackRating))
        obj.put("end_time", endTimeSec)
        obj.put("rated", rated)
        obj.put("time_class", timeClass)
        obj.put("time_control", timeControl)
        return obj
    }

    // ---- parseChessComGame: basic parsing ----

    @Test
    fun `parseChessComGame returns FetchedGame for valid json`() {
        val result = ChessComGameFetcher.parseChessComGame(gameJson(), "playerA")
        assertNotNull(result)
    }

    @Test
    fun `parseChessComGame uses uuid as platformGameId`() {
        val result = ChessComGameFetcher.parseChessComGame(gameJson(uuid = "my-uuid"), "playerA")!!
        assertEquals("my-uuid", result.platformGameId)
    }

    @Test
    fun `parseChessComGame falls back to url when uuid absent`() {
        val result = ChessComGameFetcher.parseChessComGame(
            gameJson(uuid = null, url = "https://chess.com/game/fallback"),
            "playerA"
        )!!
        assertEquals("https://chess.com/game/fallback", result.platformGameId)
    }

    @Test
    fun `parseChessComGame sets isUserWhite true when user is white`() {
        val result = ChessComGameFetcher.parseChessComGame(gameJson(), "playerA")!!
        assertTrue(result.isUserWhite)
    }

    @Test
    fun `parseChessComGame sets isUserWhite false when user is black`() {
        val result = ChessComGameFetcher.parseChessComGame(gameJson(), "playerB")!!
        assertFalse(result.isUserWhite)
    }

    @Test
    fun `parseChessComGame opponent is black player when user is white`() {
        val result = ChessComGameFetcher.parseChessComGame(gameJson(blackUser = "enemy99"), "playerA")!!
        assertEquals("enemy99", result.opponentName)
    }

    @Test
    fun `parseChessComGame opponent is white player when user is black`() {
        val result = ChessComGameFetcher.parseChessComGame(gameJson(whiteUser = "enemy99"), "playerB")!!
        assertEquals("enemy99", result.opponentName)
    }

    @Test
    fun `parseChessComGame extracts result from PGN headers`() {
        val result = ChessComGameFetcher.parseChessComGame(gameJson(), "playerA")!!
        assertEquals("1-0", result.result)
    }

    @Test
    fun `parseChessComGame extracts rated flag`() {
        assertTrue(ChessComGameFetcher.parseChessComGame(gameJson(rated = true), "playerA")!!.rated)
        assertFalse(ChessComGameFetcher.parseChessComGame(gameJson(rated = false), "playerA")!!.rated)
    }

    @Test
    fun `parseChessComGame playedAt is end_time multiplied by 1000`() {
        val result = ChessComGameFetcher.parseChessComGame(gameJson(endTimeSec = 1_700_000_000L), "playerA")!!
        assertEquals(1_700_000_000_000L, result.playedAt)
    }

    @Test
    fun `parseChessComGame extracts opening from PGN Opening header`() {
        val result = ChessComGameFetcher.parseChessComGame(gameJson(), "playerA")!!
        assertEquals("Italian Game", result.opening)
    }

    @Test
    fun `parseChessComGame extracts opening from ECO header when no Opening header`() {
        val pgn = "[Event \"Live Chess\"]\n[White \"playerA\"]\n[Black \"playerB\"]\n" +
                "[Result \"1-0\"]\n[ECO \"C50\"]\n\n1. e4 e5 1-0"
        val result = ChessComGameFetcher.parseChessComGame(gameJson(pgn = pgn), "playerA")!!
        assertEquals("C50", result.opening)
    }

    // ---- ratings ----

    @Test
    fun `parseChessComGame playerRating is white rating when user is white`() {
        val result = ChessComGameFetcher.parseChessComGame(gameJson(whiteRating = 1750, blackRating = 1800), "playerA")!!
        assertEquals(1750, result.playerRating)
        assertEquals(1800, result.opponentRating)
    }

    @Test
    fun `parseChessComGame playerRating is black rating when user is black`() {
        val result = ChessComGameFetcher.parseChessComGame(gameJson(whiteRating = 1750, blackRating = 1800), "playerB")!!
        assertEquals(1800, result.playerRating)
        assertEquals(1750, result.opponentRating)
    }

    // ---- time control ----

    @Test
    fun `parseChessComGame time control from PGN header takes precedence`() {
        val result = ChessComGameFetcher.parseChessComGame(gameJson(), "playerA")!!
        assertEquals("600", result.timeControl)
    }

    @Test
    fun `parseChessComGame time control falls back to json field when no PGN header`() {
        val pgn = "[Event \"?\"]\n[White \"playerA\"]\n[Black \"playerB\"]\n[Result \"1-0\"]\n\n1. e4 1-0"
        val result = ChessComGameFetcher.parseChessComGame(gameJson(pgn = pgn, timeControl = "300+2"), "playerA")!!
        assertEquals("300+2", result.timeControl)
    }

    // ---- time class / category ----

    @Test
    fun `parseChessComGame rapid time_class maps to rapid`() {
        val result = ChessComGameFetcher.parseChessComGame(gameJson(timeClass = "rapid"), "playerA")!!
        assertEquals("rapid", result.timeCategory)
    }

    @Test
    fun `parseChessComGame blitz time_class maps to blitz`() {
        val result = ChessComGameFetcher.parseChessComGame(gameJson(timeClass = "blitz"), "playerA")!!
        assertEquals("blitz", result.timeCategory)
    }

    @Test
    fun `parseChessComGame bullet time_class maps to bullet`() {
        val result = ChessComGameFetcher.parseChessComGame(gameJson(timeClass = "bullet"), "playerA")!!
        assertEquals("bullet", result.timeCategory)
    }

    @Test
    fun `parseChessComGame daily time_class maps to classical`() {
        val result = ChessComGameFetcher.parseChessComGame(gameJson(timeClass = "daily"), "playerA")!!
        assertEquals("classical", result.timeCategory)
    }

    @Test
    fun `parseChessComGame unknown time_class returns null category`() {
        val result = ChessComGameFetcher.parseChessComGame(gameJson(timeClass = "unknown"), "playerA")!!
        assertNull(result.timeCategory)
    }

    // ---- null / blank guards ----

    @Test
    fun `parseChessComGame null json returns null`() {
        assertNull(ChessComGameFetcher.parseChessComGame(null, "playerA"))
    }

    @Test
    fun `parseChessComGame blank pgn returns null`() {
        val result = ChessComGameFetcher.parseChessComGame(gameJson(pgn = ""), "playerA")
        assertNull(result)
    }

    @Test
    fun `parseChessComGame user not in game returns null`() {
        val result = ChessComGameFetcher.parseChessComGame(gameJson(), "unknownUser")
        assertNull(result)
    }

    @Test
    fun `parseChessComGame case-insensitive username matching`() {
        val result = ChessComGameFetcher.parseChessComGame(gameJson(whiteUser = "PlayerA"), "playera")
        assertNotNull(result)
        assertTrue(result!!.isUserWhite)
    }

    // ---- epochMsToYearMonth ----

    @Test
    fun `epochMsToYearMonth January 2024`() {
        // 2024-01-15 00:00:00 UTC in ms
        val epochMs = 1_705_276_800_000L  // approx 2024-01-15
        val result = ChessComGameFetcher.epochMsToYearMonth(epochMs)
        assertEquals(202401, result)
    }

    @Test
    fun `epochMsToYearMonth December 2023`() {
        // 2023-12-01 00:00:00 UTC
        val epochMs = 1_701_388_800_000L
        val result = ChessComGameFetcher.epochMsToYearMonth(epochMs)
        assertEquals(202312, result)
    }

    @Test
    fun `epochMsToYearMonth encodes year times 100 plus month`() {
        // 2020-06-01
        val epochMs = 1_590_969_600_000L
        val result = ChessComGameFetcher.epochMsToYearMonth(epochMs)
        assertEquals(202006, result)
    }

    // ---- archiveUrlYearMonth ----

    @Test
    fun `archiveUrlYearMonth parses standard chess com archive url`() {
        val url = "https://api.chess.com/pub/player/magnus/games/2024/01"
        assertEquals(202401, ChessComGameFetcher.archiveUrlYearMonth(url))
    }

    @Test
    fun `archiveUrlYearMonth parses url with trailing slash`() {
        val url = "https://api.chess.com/pub/player/magnus/games/2023/12/"
        assertEquals(202312, ChessComGameFetcher.archiveUrlYearMonth(url))
    }

    @Test
    fun `archiveUrlYearMonth returns 0 for malformed url`() {
        assertEquals(0, ChessComGameFetcher.archiveUrlYearMonth("not-a-url"))
    }

    // ---- mapTimeClassToCategory standalone ----

    @Test
    fun `mapTimeClassToCategory is case-insensitive`() {
        assertEquals("blitz", ChessComGameFetcher.mapTimeClassToCategory("BLITZ"))
        assertEquals("rapid", ChessComGameFetcher.mapTimeClassToCategory("Rapid"))
    }

    @Test
    fun `mapTimeClassToCategory null returns null`() {
        assertNull(ChessComGameFetcher.mapTimeClassToCategory(null))
    }
}
