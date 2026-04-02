package com.example.chessrepertoiretrainer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.chessrepertoiretrainer.navigation.AppNavigation
import com.example.chessrepertoiretrainer.ui.theme.ChessRepertoireTrainerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChessRepertoireTrainerTheme {
                // Wywołujemy naszą główną funkcję nawigacji
                AppNavigation()
            }
        }
    }
}
