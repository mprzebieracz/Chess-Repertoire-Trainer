package com.example.chessrepertoiretrainer

import android.app.Application
import com.example.chessrepertoiretrainer.data.ChessDatabase

class ChessApplication : Application() {
    val database: ChessDatabase by lazy { ChessDatabase.getDatabase(this) }
}
