package com.example.chessrepertoiretrainer.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "move_eval",
    primaryKeys = ["gameId", "moveIndex"],
    foreignKeys = [ForeignKey(
        entity = SavedGame::class,
        parentColumns = ["id"],
        childColumns = ["gameId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class MoveEval(
    val gameId: String,
    val moveIndex: Int,
    val fen: String,
    val centipawns: Int?,
    val mateIn: Int?,
    val depth: Int,
    val classification: String?
)
