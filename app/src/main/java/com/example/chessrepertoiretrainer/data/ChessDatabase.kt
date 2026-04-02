package com.example.chessrepertoiretrainer.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Repertoire::class, Chapter::class, Line::class, LineMove::class, RepertoireMove::class], version = 4, exportSchema = false)
abstract class ChessDatabase : RoomDatabase() {
    abstract fun repertoireDao(): RepertoireDao

    companion object {
        @Volatile
        private var Instance: ChessDatabase? = null

        fun getDatabase(context: Context): ChessDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, ChessDatabase::class.java, "chess_database")
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { Instance = it }
            }
        }
    }
}
