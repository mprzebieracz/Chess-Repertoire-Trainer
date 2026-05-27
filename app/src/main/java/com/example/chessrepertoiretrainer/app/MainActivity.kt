package com.example.chessrepertoiretrainer.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.core.content.edit
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.chessrepertoiretrainer.core.chess.ui.BlueBoardThemeColors
import com.example.chessrepertoiretrainer.core.chess.ui.BrownBoardThemeColors
import com.example.chessrepertoiretrainer.core.chess.ui.ClassicBoardThemeColors
import com.example.chessrepertoiretrainer.core.chess.ui.LocalBoardThemeColors
import com.example.chessrepertoiretrainer.core.chess.ui.NightBoardThemeColors
import com.example.chessrepertoiretrainer.core.chess.ui.RedBoardThemeColors
import com.example.chessrepertoiretrainer.core.navigation.AppNavigation
import com.example.chessrepertoiretrainer.core.opening.OpeningClassifier
import com.example.chessrepertoiretrainer.core.ui.theme.ChessRepertoireTrainerTheme
import com.example.chessrepertoiretrainer.feature.settings.data.AppThemeMode
import com.example.chessrepertoiretrainer.feature.settings.data.BoardTheme
import com.example.chessrepertoiretrainer.feature.settings.domain.usecase.UpdateSettingUseCase
import com.example.chessrepertoiretrainer.feature.settings.presentation.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        runEcoBackfillIfNeeded()
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

    private fun runEcoBackfillIfNeeded() {
        val prefs = getSharedPreferences("backfill", MODE_PRIVATE)
        if (prefs.getBoolean("eco_v1_done", false)) return
        val appContainer = (application as ChessApplication).appContainer
        lifecycleScope.launch(Dispatchers.IO) {
            val games = appContainer.savedGameRepository.getGamesWithNullEcoCode()
            games.forEach { game ->
                val entry = OpeningClassifier.classify(game.pgn, appContainer.openingRegistry)
                if (entry != null) appContainer.savedGameRepository.updateEcoCode(
                    game.id,
                    entry.eco
                )
            }
            prefs.edit { putBoolean("eco_v1_done", true) }
        }
    }
}