package com.example.chessrepertoiretrainer.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.chessrepertoiretrainer.feature.settings.data.AppThemeMode
import com.example.chessrepertoiretrainer.feature.settings.data.BoardTheme
import com.example.chessrepertoiretrainer.feature.settings.data.UserSettings
import com.example.chessrepertoiretrainer.feature.settings.data.UserSettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: UserSettingsRepository
) : ViewModel() {

    val settings: StateFlow<UserSettings> = repository.settingsFlow.stateIn(
        scope = viewModelScope, started = SharingStarted.Companion.WhileSubscribed(5_000), initialValue = UserSettings()
    )

    fun updateLichessUsername(username: String) {
        viewModelScope.launch {
            repository.updateLichessUsername(username)
        }
    }

    fun updateChessComUsername(username: String) {
        viewModelScope.launch {
            repository.updateChessComUsername(username)
        }
    }

    fun updateAppThemeMode(mode: AppThemeMode) {
        viewModelScope.launch {
            repository.updateAppThemeMode(mode)
        }
    }

    fun updateBoardTheme(theme: BoardTheme) {
        viewModelScope.launch {
            repository.updateBoardTheme(theme)
        }
    }

    fun updateUseDynamicColors(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateUseDynamicColors(enabled)
        }
    }

    fun updateDefaultOnlinePlatform(platform: String) {
        viewModelScope.launch {
            repository.updateDefaultOnlinePlatform(platform)
        }
    }

    class Factory(
        private val repository: UserSettingsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            return SettingsViewModel(repository) as T
        }
    }
}