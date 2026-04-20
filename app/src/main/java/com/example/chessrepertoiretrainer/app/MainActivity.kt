package com.example.chessrepertoiretrainer.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.chessrepertoiretrainer.core.navigation.AppNavigation
import com.example.chessrepertoiretrainer.core.ui.theme.ChessRepertoireTrainerTheme
import com.example.chessrepertoiretrainer.feature.settings.domain.usecase.UpdateSettingUseCase
import com.example.chessrepertoiretrainer.feature.settings.data.AppThemeMode
import com.example.chessrepertoiretrainer.feature.settings.data.BoardTheme
import com.example.chessrepertoiretrainer.feature.settings.presentation.SettingsViewModel
import com.example.chessrepertoiretrainer.core.chess.ui.BlueBoardThemeColors
import com.example.chessrepertoiretrainer.core.chess.ui.BrownBoardThemeColors
import com.example.chessrepertoiretrainer.core.chess.ui.ClassicBoardThemeColors
import com.example.chessrepertoiretrainer.core.chess.ui.LocalBoardThemeColors

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val repository = (application as ChessApplication).appContainer.userSettingsRepository
            val updateSettingUseCase = UpdateSettingUseCase(repository)
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModel.Factory(repository, updateSettingUseCase)
            )

            val settingsState by settingsViewModel.settings.collectAsStateWithLifecycle()

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
                darkTheme = darkTheme ?: isSystemInDarkTheme(), dynamicColor = useDynamic
            ) {
                CompositionLocalProvider(
                    LocalBoardThemeColors provides boardColors
                ) {
                    AppNavigation(settingsViewModel)
                }
            }
        }
    }
}