package com.example.chessrepertoiretrainer.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "repertoire_moves",
    foreignKeys = [
        ForeignKey(
            entity = Chapter::class,
            parentColumns = ["id"],
            childColumns = ["chapterId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("chapterId")]
)
data class RepertoireMove(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val chapterId: Int,
    val parentId: Long?,
    val moveSan: String,
    val positionFen: String
)
