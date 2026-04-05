package com.example.chessrepertoiretrainer.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Lightweight per-game statistics derived from the stored PGN and Game
 * metadata. This keeps aggregation queries fast without needing to parse
 * PGNs on every request.
 */
@Entity(
    tableName = "game_stats",
    foreignKeys = [
        ForeignKey(
            entity = Game::class,
            parentColumns = ["id"],
            childColumns = ["gameId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("gameId"),
        Index("profileId"),
        Index("openingKey"),
        Index("color"),
        Index("timeCategory")
    ]
)
data class GameStats(
    @PrimaryKey val gameId: Long,
    val profileId: Long,
    val openingKey: String,
    val color: String,          // "white" or "black" from the user's perspective
    val timeCategory: String,   // e.g. "bullet", "blitz", "rapid", "classical"
    val result: String,         // "win", "draw", or "loss" from the user's perspective
    val isRated: Boolean,
    val plyCount: Int,
    val userMoveCount: Int,
    val opponentMoveCount: Int
)

