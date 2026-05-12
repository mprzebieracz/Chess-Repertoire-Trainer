package com.example.chessrepertoiretrainer.feature.settings.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private const val SETTINGS_DATASTORE_NAME = "user_settings"

private val Context.settingsDataStore by preferencesDataStore(name = SETTINGS_DATASTORE_NAME)

enum class AppThemeMode { SYSTEM, LIGHT, DARK }

enum class AppColorTheme { WARM_BROWN, FOREST_GREEN, WARM_CREAM, VELVET_PINK }

enum class BoardTheme { CLASSIC, BLUE, BROWN, RED, NIGHT }

data class UserSettings(
    val lichessUsername: String = "",
    val chessComUsername: String = "",
    val appThemeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val appColorTheme: AppColorTheme = AppColorTheme.WARM_BROWN,
    val boardTheme: BoardTheme = BoardTheme.CLASSIC,
    val useDynamicColors: Boolean = true,
    val defaultOnlinePlatform: String = "lichess",
    val lichessLastSyncAt: Long = 0L,
    val chessComLastSyncAt: Long = 0L,
    val engineDepth: Int = 18,
    val engineMovetime: Int = 2000,
    val engineThreads: Int = 2
)

class UserSettingsRepository(private val context: Context) {

    private object Keys {
        val LICHESS_USERNAME = stringPreferencesKey("lichess_username")
        val CHESSCOM_USERNAME = stringPreferencesKey("chesscom_username")
        val APP_THEME_MODE = stringPreferencesKey("app_theme_mode")
        val APP_COLOR_THEME = stringPreferencesKey("app_color_theme")
        val BOARD_THEME = stringPreferencesKey("board_theme")
        val USE_DYNAMIC_COLORS = booleanPreferencesKey("use_dynamic_colors")
        val DEFAULT_ONLINE_PLATFORM = stringPreferencesKey("default_online_platform")
        val LICHESS_LAST_SYNC_AT = longPreferencesKey("lichess_last_sync_at")
        val CHESSCOM_LAST_SYNC_AT = longPreferencesKey("chesscom_last_sync_at")
        val ENGINE_DEPTH = intPreferencesKey("engine_depth")
        val ENGINE_MOVETIME = intPreferencesKey("engine_movetime")
        val ENGINE_THREADS = intPreferencesKey("engine_threads")
    }

    val settingsFlow: Flow<UserSettings> = context.settingsDataStore.data.catch { exception ->
        if (exception is IOException) {
            emit(emptyPreferences())
        } else {
            throw exception
        }
    }.map { prefs ->
        fun parseAppColorTheme(stored: String?): AppColorTheme = when (stored) {
            "DARK_WOOD", "WARM_BROWN" -> AppColorTheme.WARM_BROWN
            "VERDANT", "FOREST_GREEN", "GREEN" -> AppColorTheme.FOREST_GREEN
            "WARM_LIGHT", "WARM_CREAM" -> AppColorTheme.WARM_CREAM
            "CHESSLY_PINK", "VELVET_PINK" -> AppColorTheme.VELVET_PINK
            null -> AppColorTheme.WARM_BROWN
            else -> runCatching { AppColorTheme.valueOf(stored) }.getOrDefault(AppColorTheme.WARM_BROWN)
        }

        UserSettings(
            lichessUsername = prefs[Keys.LICHESS_USERNAME] ?: "",
            chessComUsername = prefs[Keys.CHESSCOM_USERNAME] ?: "",
            appThemeMode = prefs[Keys.APP_THEME_MODE]?.let { stored ->
                runCatching { AppThemeMode.valueOf(stored) }.getOrDefault(AppThemeMode.SYSTEM)
            } ?: AppThemeMode.SYSTEM,
            appColorTheme = parseAppColorTheme(prefs[Keys.APP_COLOR_THEME]),
            boardTheme = prefs[Keys.BOARD_THEME]?.let { stored ->
                runCatching { BoardTheme.valueOf(stored) }.getOrDefault(BoardTheme.CLASSIC)
            } ?: BoardTheme.CLASSIC,
            useDynamicColors = prefs[Keys.USE_DYNAMIC_COLORS] ?: true,
            defaultOnlinePlatform = prefs[Keys.DEFAULT_ONLINE_PLATFORM] ?: "lichess",
            lichessLastSyncAt = prefs[Keys.LICHESS_LAST_SYNC_AT] ?: 0L,
            chessComLastSyncAt = prefs[Keys.CHESSCOM_LAST_SYNC_AT] ?: 0L,
            engineDepth = prefs[Keys.ENGINE_DEPTH] ?: 20,
            engineMovetime = prefs[Keys.ENGINE_MOVETIME] ?: 2000,
            engineThreads = prefs[Keys.ENGINE_THREADS] ?: 2)
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

    suspend fun updateAppColorTheme(theme: AppColorTheme) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.APP_COLOR_THEME] = theme.name
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

    suspend fun updateLichessLastSyncAt(timestamp: Long) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.LICHESS_LAST_SYNC_AT] = timestamp
        }
    }

    suspend fun updateChessComLastSyncAt(timestamp: Long) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.CHESSCOM_LAST_SYNC_AT] = timestamp
        }
    }

    suspend fun updateEngineDepth(depth: Int) {
        context.settingsDataStore.edit { prefs -> prefs[Keys.ENGINE_DEPTH] = depth }
    }

    suspend fun updateEngineMovetime(ms: Int) {
        context.settingsDataStore.edit { prefs -> prefs[Keys.ENGINE_MOVETIME] = ms }
    }

    suspend fun updateEngineThreads(threads: Int) {
        context.settingsDataStore.edit { prefs -> prefs[Keys.ENGINE_THREADS] = threads }
    }
}