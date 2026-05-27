package com.example.chessrepertoiretrainer.core.network.games

import org.junit.Assert.*
import org.junit.Test

class LichessGameFetcherTest {

    // ---- helpers ----

    private fun gameJson(
        id: String = "abc123",
        rated: Boolean = true,
        speed: String = "rapid",
        pgn: String = SAMPLE_PGN,
        whiteUser: String = "playerA",
        blackUser: String = "playerB",
        whiteRating: Int = 1500,
        blackRating: Int = 1600,
        lastMoveAt: Long = 1_700_000_000_000L,
        openingName: String? = "Italian Game",
        clockInitial: Long? = 600L,
        clockIncrement: Long? = 0L
    ): String {
        val openingBlock = if (openingName != null) """"opening":{"name":"$openingName"}""" else """"opening":null"""
        val clockBlock = if (clockInitial != null)
            """"clock":{"initial":$clockInitial,"increment":${clockIncrement ?: 0}}"""
        else
            """"clock":null"""
        return """
            {
              "id": "$id",
              "rated": $rated,
              "speed": "$speed",
              "pgn": ${escapeJson(pgn)},
              "players": {
                "white": { "user": { "name": "$whiteUser" }, "rating": $whiteRating },
                "black": { "user": { "name": "$blackUser" }, "rating": $blackRating }
              },
              "lastMoveAt": $lastMoveAt,
              $openingBlock,
              $clockBlock
            }
        """.trimIndent()
    }

    private fun escapeJson(s: String) = "\"${s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")}\""

    private val SAMPLE_PGN = "[Event \"?\"]\n[White \"playerA\"]\n[Black \"playerB\"]\n[Result \"1-0\"]\n[TimeControl \"600+0\"]\n\n1. e4 e5 1-0"

    // ---- parseLichessGame: basic parsing ----

    @Test
    fun `parseLichessGame returns FetchedGame for valid json`() {
        val result = LichessGameFetcher.parseLichessGame(gameJson(), "playerA", ratedOnly = false)
        assertNotNull(result)
    }

    @Test
    fun `parseLichessGame uses id field as platformGameId`() {
        val result = LichessGameFetcher.parseLichessGame(gameJson(id = "xyz999"), "playerA", ratedOnly = false)!!
        assertEquals("xyz999", result.platformGameId)
    }

    @Test
    fun `parseLichessGame sets isUserWhite true when user is white`() {
        val result = LichessGameFetcher.parseLichessGame(gameJson(), "playerA", ratedOnly = false)!!
        assertTrue(result.isUserWhite)
    }

    @Test
    fun `parseLichessGame sets isUserWhite false when user is black`() {
        val result = LichessGameFetcher.parseLichessGame(gameJson(), "playerB", ratedOnly = false)!!
        assertFalse(result.isUserWhite)
    }

    @Test
    fun `parseLichessGame opponent is black player when user is white`() {
        val result = LichessGameFetcher.parseLichessGame(gameJson(blackUser = "opponent123"), "playerA", ratedOnly = false)!!
        assertEquals("opponent123", result.opponentName)
    }

    @Test
    fun `parseLichessGame opponent is white player when user is black`() {
        val result = LichessGameFetcher.parseLichessGame(gameJson(whiteUser = "opponent123"), "playerB", ratedOnly = false)!!
        assertEquals("opponent123", result.opponentName)
    }

    @Test
    fun `parseLichessGame extracts result from PGN headers`() {
        val result = LichessGameFetcher.parseLichessGame(gameJson(), "playerA", ratedOnly = false)!!
        assertEquals("1-0", result.result)
    }

    @Test
    fun `parseLichessGame extracts rated flag`() {
        val rated = LichessGameFetcher.parseLichessGame(gameJson(rated = true), "playerA", ratedOnly = false)!!
        assertTrue(rated.rated)

        val unrated = LichessGameFetcher.parseLichessGame(gameJson(rated = false), "playerA", ratedOnly = false)!!
        assertFalse(unrated.rated)
    }

    @Test
    fun `parseLichessGame extracts lastMoveAt as playedAt`() {
        val result = LichessGameFetcher.parseLichessGame(gameJson(lastMoveAt = 1_700_000_000_000L), "playerA", ratedOnly = false)!!
        assertEquals(1_700_000_000_000L, result.playedAt)
    }

    @Test
    fun `parseLichessGame extracts opening name`() {
        val result = LichessGameFetcher.parseLichessGame(gameJson(openingName = "Sicilian Defense"), "playerA", ratedOnly = false)!!
        assertEquals("Sicilian Defense", result.opening)
    }

    @Test
    fun `parseLichessGame null opening returns null opening field`() {
        val result = LichessGameFetcher.parseLichessGame(gameJson(openingName = null), "playerA", ratedOnly = false)!!
        assertNull(result.opening)
    }

    // ---- ratings ----

    @Test
    fun `parseLichessGame playerRating is white rating when user is white`() {
        val result = LichessGameFetcher.parseLichessGame(gameJson(whiteRating = 1750, blackRating = 1800), "playerA", ratedOnly = false)!!
        assertEquals(1750, result.playerRating)
        assertEquals(1800, result.opponentRating)
    }

    @Test
    fun `parseLichessGame playerRating is black rating when user is black`() {
        val result = LichessGameFetcher.parseLichessGame(gameJson(whiteRating = 1750, blackRating = 1800), "playerB", ratedOnly = false)!!
        assertEquals(1800, result.playerRating)
        assertEquals(1750, result.opponentRating)
    }

    // ---- time control ----

    @Test
    fun `parseLichessGame time control from PGN header takes precedence`() {
        val result = LichessGameFetcher.parseLichessGame(gameJson(), "playerA", ratedOnly = false)!!
        assertEquals("600+0", result.timeControl)
    }

    @Test
    fun `parseLichessGame time control from clock when no PGN TimeControl header`() {
        val pgn = "[Event \"?\"]\n[White \"playerA\"]\n[Black \"playerB\"]\n[Result \"1-0\"]\n\n1. e4 1-0"
        val result = LichessGameFetcher.parseLichessGame(
            gameJson(pgn = pgn, clockInitial = 300L, clockIncrement = 3L),
            "playerA",
            ratedOnly = false
        )!!
        assertEquals("300+3", result.timeControl)
    }

    // ---- speed / time category ----

    @Test
    fun `parseLichessGame rapid speed maps to rapid category`() {
        val result = LichessGameFetcher.parseLichessGame(gameJson(speed = "rapid"), "playerA", ratedOnly = false)!!
        assertEquals("rapid", result.timeCategory)
    }

    @Test
    fun `parseLichessGame blitz speed maps to blitz category`() {
        val result = LichessGameFetcher.parseLichessGame(gameJson(speed = "blitz"), "playerA", ratedOnly = false)!!
        assertEquals("blitz", result.timeCategory)
    }

    @Test
    fun `parseLichessGame bullet speed maps to bullet category`() {
        val result = LichessGameFetcher.parseLichessGame(gameJson(speed = "bullet"), "playerA", ratedOnly = false)!!
        assertEquals("bullet", result.timeCategory)
    }

    @Test
    fun `parseLichessGame ultrabullet maps to bullet category`() {
        val result = LichessGameFetcher.parseLichessGame(gameJson(speed = "ultrabullet"), "playerA", ratedOnly = false)!!
        assertEquals("bullet", result.timeCategory)
    }

    @Test
    fun `parseLichessGame classical speed maps to classical category`() {
        val result = LichessGameFetcher.parseLichessGame(gameJson(speed = "classical"), "playerA", ratedOnly = false)!!
        assertEquals("classical", result.timeCategory)
    }

    @Test
    fun `parseLichessGame correspondence maps to classical category`() {
        val result = LichessGameFetcher.parseLichessGame(gameJson(speed = "correspondence"), "playerA", ratedOnly = false)!!
        assertEquals("classical", result.timeCategory)
    }

    @Test
    fun `parseLichessGame unknown speed returns null category`() {
        val result = LichessGameFetcher.parseLichessGame(gameJson(speed = ""), "playerA", ratedOnly = false)!!
        assertNull(result.timeCategory)
    }

    // ---- filter: ratedOnly ----

    @Test
    fun `parseLichessGame ratedOnly=true filters out unrated game`() {
        val result = LichessGameFetcher.parseLichessGame(gameJson(rated = false), "playerA", ratedOnly = true)
        assertNull(result)
    }

    @Test
    fun `parseLichessGame ratedOnly=true passes rated game`() {
        val result = LichessGameFetcher.parseLichessGame(gameJson(rated = true), "playerA", ratedOnly = true)
        assertNotNull(result)
    }

    // ---- null / blank guards ----

    @Test
    fun `parseLichessGame blank line returns null`() {
        assertNull(LichessGameFetcher.parseLichessGame("", "playerA", ratedOnly = false))
        assertNull(LichessGameFetcher.parseLichessGame("   ", "playerA", ratedOnly = false))
    }

    @Test
    fun `parseLichessGame user not in game returns null`() {
        val result = LichessGameFetcher.parseLichessGame(gameJson(), "unknownUser", ratedOnly = false)
        assertNull(result)
    }

    @Test
    fun `parseLichessGame missing pgn returns null`() {
        val json = """{"id":"x","rated":true,"speed":"rapid","players":{"white":{"user":{"name":"a"},"rating":1500},"black":{"user":{"name":"b"},"rating":1500}},"lastMoveAt":1000}"""
        assertNull(LichessGameFetcher.parseLichessGame(json, "a", ratedOnly = false))
    }

    @Test
    fun `parseLichessGame case-insensitive username matching`() {
        val result = LichessGameFetcher.parseLichessGame(gameJson(whiteUser = "PlayerA"), "playera", ratedOnly = false)
        assertNotNull(result)
        assertTrue(result!!.isUserWhite)
    }

    // ---- buildRequestUrl ----

    @Test
    fun `buildRequestUrl contains base path`() {
        val url = LichessGameFetcher.buildRequestUrl("magnus", null, "both", "", false, null)
        assertTrue(url.startsWith("https://lichess.org/api/games/user/magnus"))
    }

    @Test
    fun `buildRequestUrl includes max when provided`() {
        val url = LichessGameFetcher.buildRequestUrl("magnus", 50, "both", "", false, null)
        assertTrue(url.contains("max=50"))
    }

    @Test
    fun `buildRequestUrl omits max when null`() {
        val url = LichessGameFetcher.buildRequestUrl("magnus", null, "both", "", false, null)
        assertFalse(url.contains("max="))
    }

    @Test
    fun `buildRequestUrl includes color when white`() {
        val url = LichessGameFetcher.buildRequestUrl("magnus", null, "white", "", false, null)
        assertTrue(url.contains("color=white"))
    }

    @Test
    fun `buildRequestUrl includes color when black`() {
        val url = LichessGameFetcher.buildRequestUrl("magnus", null, "black", "", false, null)
        assertTrue(url.contains("color=black"))
    }

    @Test
    fun `buildRequestUrl omits color when both`() {
        val url = LichessGameFetcher.buildRequestUrl("magnus", null, "both", "", false, null)
        assertFalse(url.contains("color="))
    }

    @Test
    fun `buildRequestUrl includes rated=true when ratedOnly`() {
        val url = LichessGameFetcher.buildRequestUrl("magnus", null, "both", "", true, null)
        assertTrue(url.contains("rated=true"))
    }

    @Test
    fun `buildRequestUrl includes since when provided`() {
        val url = LichessGameFetcher.buildRequestUrl("magnus", null, "both", "", false, 1_700_000_000_000L)
        assertTrue(url.contains("since=1700000000000"))
    }

    @Test
    fun `buildRequestUrl includes perfType for time control filter`() {
        val url = LichessGameFetcher.buildRequestUrl("magnus", null, "both", "rapid,blitz", false, null)
        assertTrue(url.contains("perfType=rapid,blitz"))
    }

    @Test
    fun `buildRequestUrl always includes pgnInJson and opening params`() {
        val url = LichessGameFetcher.buildRequestUrl("magnus", null, "both", "", false, null)
        assertTrue(url.contains("pgnInJson=true"))
        assertTrue(url.contains("opening=true"))
    }

    // ---- mapSpeedToCategory standalone ----

    @Test
    fun `mapSpeedToCategory handles null`() {
        assertNull(LichessGameFetcher.mapSpeedToCategory(null))
    }

    @Test
    fun `mapSpeedToCategory is case-insensitive`() {
        assertEquals("blitz", LichessGameFetcher.mapSpeedToCategory("BLITZ"))
        assertEquals("rapid", LichessGameFetcher.mapSpeedToCategory("Rapid"))
    }
}
