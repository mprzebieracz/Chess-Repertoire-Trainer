package com.example.chessrepertoiretrainer.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * Stores the full move text for a single game (e.g. PGN). This keeps the
 * main Game table small while allowing us to analyze moves when building
 * the opening tree.
 */
@Entity(
    tableName = "game_moves",
    foreignKeys = [
        ForeignKey(
            entity = Game::class,
            parentColumns = ["id"],
            childColumns = ["gameId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class GameMoves(
    @PrimaryKey val gameId: Long,
    val pgn: String
)

