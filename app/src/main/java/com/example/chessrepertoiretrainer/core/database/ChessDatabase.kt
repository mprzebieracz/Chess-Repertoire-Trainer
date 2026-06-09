package com.example.chessrepertoiretrainer.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.chessrepertoiretrainer.core.database.dao.DailyActivityDao
import com.example.chessrepertoiretrainer.core.database.dao.LichessExplorerCacheDao
import com.example.chessrepertoiretrainer.core.database.dao.MoveEvalDao
import com.example.chessrepertoiretrainer.core.database.dao.PuzzleDao
import com.example.chessrepertoiretrainer.core.database.dao.RepertoireDao
import com.example.chessrepertoiretrainer.core.database.dao.RepertoireOpeningDao
import com.example.chessrepertoiretrainer.core.database.dao.RepertoirePositionIndexDao
import com.example.chessrepertoiretrainer.core.database.dao.ReviewLogDao
import com.example.chessrepertoiretrainer.core.database.dao.SavedGameDao
import com.example.chessrepertoiretrainer.core.database.dao.SavedGameRepertoireMatchDao
import com.example.chessrepertoiretrainer.core.database.entity.Chapter
import com.example.chessrepertoiretrainer.core.database.entity.DailyActivity
import com.example.chessrepertoiretrainer.core.database.entity.LichessExplorerCache
import com.example.chessrepertoiretrainer.core.database.entity.Line
import com.example.chessrepertoiretrainer.core.database.entity.LineMove
import com.example.chessrepertoiretrainer.core.database.entity.MoveEval
import com.example.chessrepertoiretrainer.core.database.entity.Puzzle
import com.example.chessrepertoiretrainer.core.database.entity.Repertoire
import com.example.chessrepertoiretrainer.core.database.entity.RepertoireOpening
import com.example.chessrepertoiretrainer.core.database.entity.RepertoirePositionIndex
import com.example.chessrepertoiretrainer.core.database.entity.ReviewLog
import com.example.chessrepertoiretrainer.core.database.entity.SavedGame
import com.example.chessrepertoiretrainer.core.database.entity.SavedGameRepertoireMatch

@Database(
    entities = [
        Repertoire::class,
        Chapter::class,
        Line::class,
        LineMove::class,
        Puzzle::class,
        SavedGame::class,
        RepertoirePositionIndex::class,
        DailyActivity::class,
        ReviewLog::class,
        SavedGameRepertoireMatch::class,
        MoveEval::class,
        LichessExplorerCache::class,
        RepertoireOpening::class,
    ], version = 26, exportSchema = false
)
abstract class ChessDatabase : RoomDatabase() {

    abstract fun repertoireDao(): RepertoireDao
    abstract fun puzzleDao(): PuzzleDao
    abstract fun savedGameDao(): SavedGameDao
    abstract fun repertoirePositionIndexDao(): RepertoirePositionIndexDao
    abstract fun dailyActivityDao(): DailyActivityDao
    abstract fun reviewLogDao(): ReviewLogDao
    abstract fun savedGameRepertoireMatchDao(): SavedGameRepertoireMatchDao
    abstract fun moveEvalDao(): MoveEvalDao
    abstract fun lichessExplorerCacheDao(): LichessExplorerCacheDao
    abstract fun repertoireOpeningDao(): RepertoireOpeningDao

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
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                return instance
            }
        }
    }
}