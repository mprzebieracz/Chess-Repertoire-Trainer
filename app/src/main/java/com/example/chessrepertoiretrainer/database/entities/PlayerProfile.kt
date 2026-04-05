package com.example.chessrepertoiretrainer.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a tracked player (you or an opponent) for opening-tree
 * analysis. For now it only stores basic identity and platform; more
 * fields (like ratings) can be added later.
 */
@Entity(tableName = "player_profiles")
data class PlayerProfile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val platform: String, // e.g. "lichess" or "chess.com"
    val displayName: String? = null,
    val lastSyncTime: Long? = null
)

