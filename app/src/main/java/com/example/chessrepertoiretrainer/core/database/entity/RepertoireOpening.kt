package com.example.chessrepertoiretrainer.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "repertoire_openings")
data class RepertoireOpening(
    @PrimaryKey val family: String,
    val eco: String? = null,
    val lastUpdatedAt: Long = System.currentTimeMillis(),
)
