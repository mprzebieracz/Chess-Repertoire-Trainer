package com.example.chessrepertoiretrainer.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "puzzles")
data class Puzzle(
    @PrimaryKey val id: String,
    val fen: String,
    val moves: String,
    val rating: Int,
    val themes: String,
    val isSolved: Boolean,
    val attempts: Int
)
