package com.example.chessrepertoiretrainer.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lichess_explorer_cache")
data class LichessExplorerCache(
    @PrimaryKey val cacheKey: String,
    val fen: String,
    val db: String,
    val speedsCsv: String,
    val ratingsCsv: String,
    val jsonPayload: String,
    val fetchedAt: Long
)
