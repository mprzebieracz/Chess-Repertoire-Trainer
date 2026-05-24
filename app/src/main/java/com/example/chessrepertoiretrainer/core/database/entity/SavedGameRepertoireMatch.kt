package com.example.chessrepertoiretrainer.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "saved_game_repertoire_match",
    foreignKeys = [ForeignKey(
        entity = SavedGame::class,
        parentColumns = ["id"],
        childColumns = ["gameId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("deepestChapterId")]
)
data class SavedGameRepertoireMatch(
    @PrimaryKey val gameId: String,
    val color: String,
    val deepestChapterId: Int?,
    val deepestLineId: Int?,
    val lastInBookMoveIndex: Int,
    val deviationMoveIndex: Int?,
    val playerInBookMoves: Int,
    val playerTotalMoves: Int,
    val playerDeviated: Boolean,
    val bookDepthPlies: Int,
    val computedAt: Long
)
