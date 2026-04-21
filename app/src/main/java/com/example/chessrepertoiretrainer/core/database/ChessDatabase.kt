package com.example.chessrepertoiretrainer.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.chessrepertoiretrainer.core.database.dao.GameDao
import com.example.chessrepertoiretrainer.core.database.dao.PlayerProfileDao
import com.example.chessrepertoiretrainer.core.database.dao.PuzzleDao
import com.example.chessrepertoiretrainer.core.database.dao.RepertoireDao
import com.example.chessrepertoiretrainer.core.database.entity.Chapter
import com.example.chessrepertoiretrainer.core.database.entity.Game
import com.example.chessrepertoiretrainer.core.database.entity.GameMoves
import com.example.chessrepertoiretrainer.core.database.entity.Line
import com.example.chessrepertoiretrainer.core.database.entity.LineMove
import com.example.chessrepertoiretrainer.core.database.entity.PlayerProfile
import com.example.chessrepertoiretrainer.core.database.entity.Puzzle
import com.example.chessrepertoiretrainer.core.database.entity.Repertoire

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
    ], version = 6, exportSchema = false
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
                    context.applicationContext, ChessDatabase::class.java, "chess_database"
                ).fallbackToDestructiveMigration(false).build()
                INSTANCE = instance
                return instance
            }
        }
    }
}
