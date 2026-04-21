package com.example.chessrepertoiretrainer.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "player_profiles")
data class PlayerProfile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val platform: String, // "lichess" or "chess.com"
    val displayName: String? = null,
    val lastSyncTime: Long? = null
)

