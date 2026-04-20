package com.example.chessrepertoiretrainer.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chapters", foreignKeys = [ForeignKey(
        entity = Repertoire::class, parentColumns = ["id"], childColumns = ["repertoireId"], onDelete = ForeignKey.CASCADE
    )], indices = [Index("repertoireId")]
)
data class Chapter(
    @PrimaryKey(autoGenerate = true) val id: Int = 0, val repertoireId: Int, val name: String, val sortOrder: Int
)
