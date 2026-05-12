package com.example.chessrepertoiretrainer.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "repertoire_position_index",
    indices = [Index(value = ["color", "normalizedFen"], unique = true)]
)
data class RepertoirePositionIndex(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val color: String,
    val normalizedFen: String,
    val chapterId: Int,
    val lineId: Int
)