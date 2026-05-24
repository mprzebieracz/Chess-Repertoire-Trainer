package com.example.chessrepertoiretrainer.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_activity")
data class DailyActivity(
    @PrimaryKey val date: Long,
    val linesTrained: Int = 0,
    val puzzlesSolved: Int = 0,
    val gamesImported: Int = 0,
    val lastUpdatedAt: Long
)
