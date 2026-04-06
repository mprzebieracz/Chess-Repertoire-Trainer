package com.example.chessrepertoiretrainer.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "lines",
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
data class Line(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val chapterId: Int,
    val name: String,
    // Review scheduling fields (simple SRS).
    val nextReviewDate: Long,
    val interval: Int,
    val easeFactor: Float,
    val consecutiveCorrect: Int,
    // Learning/progress tracking.
    val isLearned: Boolean = false,
    val learnedAt: Long? = null,
    val timesTrained: Int = 0,
    val lastTrainedAt: Long? = null
)
