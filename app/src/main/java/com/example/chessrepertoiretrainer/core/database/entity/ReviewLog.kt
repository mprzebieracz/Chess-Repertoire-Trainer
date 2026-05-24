package com.example.chessrepertoiretrainer.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "review_log",
    foreignKeys = [ForeignKey(
        entity = Line::class,
        parentColumns = ["id"],
        childColumns = ["lineId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("lineId"), Index("reviewedAt")]
)
data class ReviewLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val lineId: Int,
    val reviewedAt: Long,
    val grade: Int,
    val wasCorrect: Boolean,
    val msTaken: Long? = null
)
