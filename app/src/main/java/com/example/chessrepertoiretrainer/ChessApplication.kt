package com.example.chessrepertoiretrainer

import android.app.Application
import com.example.chessrepertoiretrainer.data.AppContainer

class ChessApplication : Application() {
    val appContainer: AppContainer by lazy {
        AppContainer(this)
    }
}
