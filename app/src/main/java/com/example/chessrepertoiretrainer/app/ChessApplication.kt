package com.example.chessrepertoiretrainer.app

import android.app.Application

class ChessApplication : Application() {
    val appContainer: AppContainer by lazy {
        AppContainer(this)
    }
}