package com.example.chessrepertoiretrainer.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.chessrepertoiretrainer.database.dao.GameDao
import com.example.chessrepertoiretrainer.database.dao.GameStatsDao
import com.example.chessrepertoiretrainer.database.dao.PlayerProfileDao
import com.example.chessrepertoiretrainer.database.dao.PuzzleDao
import com.example.chessrepertoiretrainer.database.dao.RepertoireDao
import com.example.chessrepertoiretrainer.database.entities.Chapter
import com.example.chessrepertoiretrainer.database.entities.Game
import com.example.chessrepertoiretrainer.database.entities.GameMoves
import com.example.chessrepertoiretrainer.database.entities.GameStats
import com.example.chessrepertoiretrainer.database.entities.Line
import com.example.chessrepertoiretrainer.database.entities.LineMove
import com.example.chessrepertoiretrainer.database.entities.PlayerProfile
import com.example.chessrepertoiretrainer.database.entities.Puzzle
import com.example.chessrepertoiretrainer.database.entities.Repertoire

@Database(
    entities = [
        Repertoire::class,
        Chapter::class,
        Line::class,
        LineMove::class,
        Puzzle::class,
        PlayerProfile::class,
        Game::class,
        GameMoves::class,
        GameStats::class,
    ],
    version = 5,
    exportSchema = false
)
abstract class ChessDatabase : RoomDatabase() {

    abstract fun repertoireDao(): RepertoireDao
    abstract fun puzzleDao(): PuzzleDao
    abstract fun playerProfileDao(): PlayerProfileDao
    abstract fun gameDao(): GameDao
    abstract fun gameStatsDao(): GameStatsDao

    companion object {
        @Volatile
        private var INSTANCE: ChessDatabase? = null

        fun getDatabase(context: Context): ChessDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ChessDatabase::class.java,
                    "chess_database"
                )
                    // Schema is still evolving; use destructive migration
                    // during development.
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
