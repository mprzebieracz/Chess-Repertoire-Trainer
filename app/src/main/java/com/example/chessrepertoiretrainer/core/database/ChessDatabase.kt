package com.example.chessrepertoiretrainer.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.chessrepertoiretrainer.core.database.dao.PuzzleDao
import com.example.chessrepertoiretrainer.core.database.dao.RepertoireDao
import com.example.chessrepertoiretrainer.core.database.dao.RepertoirePositionIndexDao
import com.example.chessrepertoiretrainer.core.database.dao.SavedGameDao
import com.example.chessrepertoiretrainer.core.database.entity.Chapter
import com.example.chessrepertoiretrainer.core.database.entity.Line
import com.example.chessrepertoiretrainer.core.database.entity.LineMove
import com.example.chessrepertoiretrainer.core.database.entity.Puzzle
import com.example.chessrepertoiretrainer.core.database.entity.Repertoire
import com.example.chessrepertoiretrainer.core.database.entity.RepertoirePositionIndex
import com.example.chessrepertoiretrainer.core.database.entity.SavedGame

@Database(
    entities = [
        Repertoire::class,
        Chapter::class,
        Line::class,
        LineMove::class,
        Puzzle::class,
        SavedGame::class,
        RepertoirePositionIndex::class,
    ], version = 18, exportSchema = false
)
abstract class ChessDatabase : RoomDatabase() {

    abstract fun repertoireDao(): RepertoireDao
    abstract fun puzzleDao(): PuzzleDao
    abstract fun savedGameDao(): SavedGameDao
    abstract fun repertoirePositionIndexDao(): RepertoirePositionIndexDao

    companion object {

        private val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE lines ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0")
            }
        }

        @Volatile
        private var INSTANCE: ChessDatabase? = null

        fun getDatabase(context: Context): ChessDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext, ChessDatabase::class.java, "chess_database"
                ).fallbackToDestructiveMigration(false)
                    .addMigrations(MIGRATION_17_18)
                    .build()
                INSTANCE = instance
                return instance
            }
        }
    }
}
