package com.example.chessrepertoiretrainer.feature.settings.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private const val SETTINGS_DATASTORE_NAME = "user_settings"

private val Context.settingsDataStore by preferencesDataStore(
    name = SETTINGS_DATASTORE_NAME
)

enum class AppThemeMode { SYSTEM, LIGHT, DARK }

enum class BoardTheme { CLASSIC, BLUE, BROWN }

data class UserSettings(
    val lichessUsername: String = "",
    val chessComUsername: String = "",
    val appThemeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val boardTheme: BoardTheme = BoardTheme.CLASSIC,
    val useDynamicColors: Boolean = true,
    val defaultOnlinePlatform: String = "lichess"
)

class UserSettingsRepository(private val context: Context) {

    private object Keys {
        val LICHESS_USERNAME = stringPreferencesKey("lichess_username")
        val CHESSCOM_USERNAME = stringPreferencesKey("chesscom_username")
        val APP_THEME_MODE = stringPreferencesKey("app_theme_mode")
        val BOARD_THEME = stringPreferencesKey("board_theme")
        val USE_DYNAMIC_COLORS = booleanPreferencesKey("use_dynamic_colors")
        val DEFAULT_ONLINE_PLATFORM = stringPreferencesKey("default_online_platform")
    }

    val settingsFlow: Flow<UserSettings> = context.settingsDataStore.data.catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            }
            else {
                throw exception
            }
        }.map { prefs ->
            UserSettings(
                lichessUsername = prefs[Keys.LICHESS_USERNAME] ?: "",
                chessComUsername = prefs[Keys.CHESSCOM_USERNAME] ?: "",
                appThemeMode = prefs[Keys.APP_THEME_MODE]?.let { stored ->
                    runCatching { AppThemeMode.valueOf(stored) }.getOrDefault(AppThemeMode.SYSTEM)
                } ?: AppThemeMode.SYSTEM,
                boardTheme = prefs[Keys.BOARD_THEME]?.let { stored ->
                    runCatching { BoardTheme.valueOf(stored) }.getOrDefault(BoardTheme.CLASSIC)
                } ?: BoardTheme.CLASSIC,
                useDynamicColors = prefs[Keys.USE_DYNAMIC_COLORS] ?: true,
                defaultOnlinePlatform = prefs[Keys.DEFAULT_ONLINE_PLATFORM] ?: "lichess")
        }

    suspend fun updateLichessUsername(username: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.LICHESS_USERNAME] = username.trim()
        }
    }

    suspend fun updateChessComUsername(username: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.CHESSCOM_USERNAME] = username.trim()
        }
    }

    suspend fun updateAppThemeMode(mode: AppThemeMode) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.APP_THEME_MODE] = mode.name
        }
    }

    suspend fun updateBoardTheme(theme: BoardTheme) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.BOARD_THEME] = theme.name
        }
    }

    suspend fun updateUseDynamicColors(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.USE_DYNAMIC_COLORS] = enabled
        }
    }

    suspend fun updateDefaultOnlinePlatform(platform: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.DEFAULT_ONLINE_PLATFORM] = platform
        }
    }
}


