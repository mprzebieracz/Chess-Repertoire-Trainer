package com.example.chessrepertoiretrainer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.chessrepertoiretrainer.data.AppThemeMode
import com.example.chessrepertoiretrainer.data.BoardTheme
import com.example.chessrepertoiretrainer.data.UserSettingsRepository
import com.example.chessrepertoiretrainer.navigation.AppNavigation
import com.example.chessrepertoiretrainer.ui.components.chess.BlueBoardThemeColors
import com.example.chessrepertoiretrainer.ui.components.chess.BrownBoardThemeColors
import com.example.chessrepertoiretrainer.ui.components.chess.ClassicBoardThemeColors
import com.example.chessrepertoiretrainer.ui.components.chess.LocalBoardThemeColors
import com.example.chessrepertoiretrainer.ui.theme.ChessRepertoireTrainerTheme
import com.example.chessrepertoiretrainer.ui.viewmodels.SettingsViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val repository = remember { UserSettingsRepository(applicationContext) }
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModel.Factory(repository)
            )

            val settingsState by settingsViewModel.settings.collectAsState()

            val darkTheme = when (settingsState.appThemeMode) {
                AppThemeMode.SYSTEM -> null
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
            }

            val boardColors = when (settingsState.boardTheme) {
                BoardTheme.CLASSIC -> ClassicBoardThemeColors
                BoardTheme.BLUE -> BlueBoardThemeColors
                BoardTheme.BROWN -> BrownBoardThemeColors
            }

            val useDynamic = settingsState.useDynamicColors

            ChessRepertoireTrainerTheme(
                darkTheme = darkTheme ?: androidx.compose.foundation.isSystemInDarkTheme(),
                dynamicColor = useDynamic
            ) {
                androidx.compose.runtime.CompositionLocalProvider(
                    LocalBoardThemeColors provides boardColors
                ) {
                    AppNavigation(settingsViewModel)
                }
            }
        }
    }
}
