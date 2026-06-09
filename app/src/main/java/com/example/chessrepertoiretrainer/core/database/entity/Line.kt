package com.example.chessrepertoiretrainer.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "lines",
    foreignKeys = [ForeignKey(
        entity = Chapter::class,
        parentColumns = ["id"],
        childColumns = ["chapterId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("chapterId")]
)
data class Line(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val chapterId: Int,
    val name: String,
    val nextReviewDate: Long,
    val interval: Int,
    val easeFactor: Float,
    val consecutiveCorrect: Int,
    val isLearned: Boolean = false,
    val learnedAt: Long? = null,
    val timesTrained: Int = 0,
    val lastTrainedAt: Long? = null,
    val sortOrder: Int = 0,
    val lastEcoCode: String? = null,
    val imagePath: String? = null,
)