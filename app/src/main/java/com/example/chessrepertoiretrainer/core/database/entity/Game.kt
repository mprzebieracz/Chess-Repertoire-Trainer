package com.example.chessrepertoiretrainer.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "games", foreignKeys = [ForeignKey(
        entity = PlayerProfile::class,
        parentColumns = ["id"],
        childColumns = ["profileId"],
        onDelete = ForeignKey.CASCADE
    )], indices = [Index(value = ["profileId"]), Index(value = ["platformGameId"], unique = false)]
)
data class Game(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val platformGameId: String,
    val profileId: Long,
    val opponentName: String,
    val isUserWhite: Boolean,
    val result: String,      // "1-0", "0-1", "1/2-1/2"
    val timeControl: String?,
    val timeCategory: String?,
    val rated: Boolean,
    val playedAt: Long
)

