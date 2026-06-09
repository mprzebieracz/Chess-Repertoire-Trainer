package com.example.chessrepertoiretrainer.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "saved_games",
    indices = [Index("playedAt"), Index("platform"), Index("timeCategory"), Index("playerUsername"), Index(
        "ecoCode"
    )]
)
data class SavedGame(
    @PrimaryKey val id: String,
    val platform: String,
    val platformGameId: String,
    val playerUsername: String,
    val opponentName: String,
    val isPlayerWhite: Boolean,
    val result: String,
    val playerResult: String,
    val timeControl: String?,
    val timeCategory: String?,
    val opening: String?,
    val playerRating: Int?,
    val opponentRating: Int?,
    val rated: Boolean,
    val playedAt: Long,
    val pgn: String,
    val ecoCode: String? = null
)