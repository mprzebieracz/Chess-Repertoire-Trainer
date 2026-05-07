package com.example.chessrepertoiretrainer.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.chessrepertoiretrainer.core.database.dao.PuzzleDao
import com.example.chessrepertoiretrainer.core.database.dao.RepertoireDao
import com.example.chessrepertoiretrainer.core.database.dao.SavedGameDao
import com.example.chessrepertoiretrainer.core.database.entity.Chapter
import com.example.chessrepertoiretrainer.core.database.entity.Line
import com.example.chessrepertoiretrainer.core.database.entity.LineMove
import com.example.chessrepertoiretrainer.core.database.entity.Puzzle
import com.example.chessrepertoiretrainer.core.database.entity.Repertoire
import com.example.chessrepertoiretrainer.core.database.entity.SavedGame

@Database(
    entities = [
        Repertoire::class,
        Chapter::class,
        Line::class,
        LineMove::class,
        Puzzle::class,
        SavedGame::class,
    ], version = 10, exportSchema = false
)
abstract class ChessDatabase : RoomDatabase() {

    abstract fun repertoireDao(): RepertoireDao
    abstract fun puzzleDao(): PuzzleDao
    abstract fun savedGameDao(): SavedGameDao

    companion object {

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE puzzles ADD COLUMN sourceDate TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS saved_games (
                        id TEXT NOT NULL PRIMARY KEY,
                        platform TEXT NOT NULL,
                        platformGameId TEXT NOT NULL,
                        playerUsername TEXT NOT NULL,
                        opponentName TEXT NOT NULL,
                        isPlayerWhite INTEGER NOT NULL,
                        result TEXT NOT NULL,
                        playerResult TEXT NOT NULL,
                        timeControl TEXT,
                        timeCategory TEXT,
                        opening TEXT,
                        rated INTEGER NOT NULL,
                        playedAt INTEGER NOT NULL,
                        pgn TEXT NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_saved_games_playedAt ON saved_games (playedAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_saved_games_platform ON saved_games (platform)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_saved_games_timeCategory ON saved_games (timeCategory)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_saved_games_playerUsername ON saved_games (playerUsername)")
            }
        }

        @Volatile
        private var INSTANCE: ChessDatabase? = null

        fun getDatabase(context: Context): ChessDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext, ChessDatabase::class.java, "chess_database"
                ).addMigrations(MIGRATION_8_9, MIGRATION_9_10).build()
                INSTANCE = instance
                return instance
            }
        }
    }
}
