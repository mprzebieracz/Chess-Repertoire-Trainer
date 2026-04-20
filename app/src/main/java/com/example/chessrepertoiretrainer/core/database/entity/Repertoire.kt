package com.example.chessrepertoiretrainer.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "repertoires")
data class Repertoire(
    @PrimaryKey(autoGenerate = true) val id: Int = 0, val name: String, val color: String
)
