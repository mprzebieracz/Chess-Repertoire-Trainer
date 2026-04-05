package com.example.chessrepertoiretrainer.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.chessrepertoiretrainer.database.dao.PuzzleDao
import com.example.chessrepertoiretrainer.database.dao.RepertoireDao
import com.example.chessrepertoiretrainer.database.dao.GameDao
import com.example.chessrepertoiretrainer.database.dao.PlayerProfileDao
import com.example.chessrepertoiretrainer.database.entities.*

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
    ],
    version = 2,
    exportSchema = false
)
abstract class ChessDatabase : RoomDatabase() {

    abstract fun repertoireDao(): RepertoireDao
    abstract fun puzzleDao(): PuzzleDao
    abstract fun playerProfileDao(): PlayerProfileDao
    abstract fun gameDao(): GameDao

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
