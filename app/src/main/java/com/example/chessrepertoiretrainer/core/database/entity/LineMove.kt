package com.example.chessrepertoiretrainer.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "line_moves",
    foreignKeys = [ForeignKey(
        entity = Line::class,
        parentColumns = ["id"],
        childColumns = ["lineId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("lineId"), Index("fen")]
)
data class LineMove(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val lineId: Int,
    val moveIndex: Int,
    val moveSan: String,
    val fen: String,
    val comment: String?,
    val arrows: String?
)