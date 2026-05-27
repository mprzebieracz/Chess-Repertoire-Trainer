package com.example.chessrepertoiretrainer.feature.mygames.data

import com.example.chessrepertoiretrainer.core.network.games.FetchedGame
import org.junit.Assert.*
import org.junit.Test

class FetchedGameMappingTest {

    private fun fetched(
        result: String,
        isUserWhite: Boolean = true,
        gameId: String = "abc123"
    ) = FetchedGame(
        platformGameId = gameId,
        opponentName = "Opponent",
        isUserWhite = isUserWhite,
        result = result,
        timeControl = "600",
        timeCategory = "rapid",
        opening = null,
        playerRating = 1500,
        opponentRating = 1500,
        rated = true,
        playedAt = 1000L,
        pgn = "1. e4 e5 $result"
    )

    // ---- playerResult mapping ----

    @Test
    fun `1-0 as white player maps to win`() {
        val saved = fetched("1-0", isUserWhite = true).toSavedGame("lichess", "user")
        assertEquals("win", saved.playerResult)
    }

    @Test
    fun `1-0 as black player maps to loss`() {
        val saved = fetched("1-0", isUserWhite = false).toSavedGame("lichess", "user")
        assertEquals("loss", saved.playerResult)
    }

    @Test
    fun `0-1 as black player maps to win`() {
        val saved = fetched("0-1", isUserWhite = false).toSavedGame("lichess", "user")
        assertEquals("win", saved.playerResult)
    }

    @Test
    fun `0-1 as white player maps to loss`() {
        val saved = fetched("0-1", isUserWhite = true).toSavedGame("lichess", "user")
        assertEquals("loss", saved.playerResult)
    }

    @Test
    fun `draw result maps to draw`() {
        val saved = fetched("1/2-1/2").toSavedGame("lichess", "user")
        assertEquals("draw", saved.playerResult)
    }

    @Test
    fun `unknown result maps to unknown`() {
        val saved = fetched("*").toSavedGame("lichess", "user")
        assertEquals("unknown", saved.playerResult)
    }

    // ---- id construction ----

    @Test
    fun `saved game id is platform underscore gameId`() {
        val saved = fetched("1-0", gameId = "myGameId").toSavedGame("lichess", "user")
        assertEquals("lichess_myGameId", saved.id)
    }

    // ---- field pass-through ----

    @Test
    fun `platform and username are stored`() {
        val saved = fetched("1-0").toSavedGame("chess.com", "magnus")
        assertEquals("chess.com", saved.platform)
        assertEquals("magnus", saved.playerUsername)
    }

    @Test
    fun `isPlayerWhite mirrors isUserWhite`() {
        assertTrue(fetched("1-0", isUserWhite = true).toSavedGame("x", "y").isPlayerWhite)
        assertFalse(fetched("1-0", isUserWhite = false).toSavedGame("x", "y").isPlayerWhite)
    }
}
