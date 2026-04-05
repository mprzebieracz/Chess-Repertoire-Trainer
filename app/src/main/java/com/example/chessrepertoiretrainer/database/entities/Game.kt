package com.example.chessrepertoiretrainer.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Basic game metadata for opening-tree analysis. Detailed moves will be
 * stored in a separate table.
 */
@Entity(
    tableName = "games",
    foreignKeys = [
        ForeignKey(
            entity = PlayerProfile::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["profileId"]),
        Index(value = ["platformGameId"], unique = false)
    ]
)
data class Game(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val platformGameId: String,
    val profileId: Long,
    val opponentName: String,
    val isUserWhite: Boolean,
    val result: String,      // "1-0", "0-1", "1/2-1/2", "*" etc.
    val timeControl: String?,
    /** Normalized time-control category such as "bullet", "blitz", "rapid", "classical". */
    val timeCategory: String?,
    val rated: Boolean,
    val playedAt: Long
)

