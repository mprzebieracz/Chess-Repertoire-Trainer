package com.example.chessrepertoiretrainer.feature.settings.domain.usecase

import com.example.chessrepertoiretrainer.feature.settings.data.AppThemeMode
import com.example.chessrepertoiretrainer.feature.settings.data.BoardTheme
import com.example.chessrepertoiretrainer.feature.settings.data.UserSettingsRepository

class UpdateSettingUseCase(private val repository: UserSettingsRepository) {
    suspend fun lichessUsername(username: String): Result<Unit> = runCatching {
        repository.updateLichessUsername(username)
    }

    suspend fun lichessApiToken(token: String): Result<Unit> = runCatching {
        repository.updateLichessApiToken(token)
    }

    suspend fun chessComUsername(username: String): Result<Unit> = runCatching {
        repository.updateChessComUsername(username)
    }

    suspend fun appTheme(mode: AppThemeMode): Result<Unit> = runCatching {
        repository.updateAppThemeMode(mode)
    }

    suspend fun boardTheme(theme: BoardTheme): Result<Unit> = runCatching {
        repository.updateBoardTheme(theme)
    }

    suspend fun dynamicColors(enabled: Boolean): Result<Unit> = runCatching {
        repository.updateUseDynamicColors(enabled)
    }

    suspend fun defaultOnlinePlatform(platform: String): Result<Unit> = runCatching {
        repository.updateDefaultOnlinePlatform(platform)
    }
}