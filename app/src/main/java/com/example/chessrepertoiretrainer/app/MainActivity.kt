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
import com.example.chessrepertoiretrainer.core.chess.ui.BlueBoardThemeColors
import com.example.chessrepertoiretrainer.core.chess.ui.BrownBoardThemeColors
import com.example.chessrepertoiretrainer.core.chess.ui.ClassicBoardThemeColors
import com.example.chessrepertoiretrainer.core.chess.ui.LocalBoardThemeColors
import com.example.chessrepertoiretrainer.core.chess.ui.NightBoardThemeColors
import com.example.chessrepertoiretrainer.core.chess.ui.RedBoardThemeColors
import com.example.chessrepertoiretrainer.core.navigation.AppNavigation
import com.example.chessrepertoiretrainer.core.ui.theme.ChessRepertoireTrainerTheme
import com.example.chessrepertoiretrainer.feature.settings.data.AppThemeMode
import com.example.chessrepertoiretrainer.feature.settings.data.BoardTheme
import com.example.chessrepertoiretrainer.feature.settings.domain.usecase.UpdateSettingUseCase
import com.example.chessrepertoiretrainer.feature.settings.presentation.SettingsViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val repository = (application as ChessApplication).appContainer.userSettingsRepository
            val updateSettingUseCase = UpdateSettingUseCase(repository)
            val settingsViewModel: SettingsViewModel =
                viewModel(factory = SettingsViewModel.Factory(repository, updateSettingUseCase))

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
                BoardTheme.RED -> RedBoardThemeColors
                BoardTheme.NIGHT -> NightBoardThemeColors
            }

            val useDynamic = settingsState.useDynamicColors

            ChessRepertoireTrainerTheme(
                darkTheme = darkTheme ?: isSystemInDarkTheme(),
                dynamicColor = useDynamic,
                appColorTheme = settingsState.appColorTheme
            ) {
                CompositionLocalProvider(LocalBoardThemeColors provides boardColors) {
                    AppNavigation(settingsViewModel)
                }
            }
        }
    }
}