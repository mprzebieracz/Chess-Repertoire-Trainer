package com.example.chessrepertoiretrainer.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.chessrepertoiretrainer.data.AppThemeMode
import com.example.chessrepertoiretrainer.data.BoardTheme
import com.example.chessrepertoiretrainer.data.UserSettings
import com.example.chessrepertoiretrainer.data.UserSettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: UserSettingsRepository
) : ViewModel() {

    val settings: StateFlow<UserSettings> = repository.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UserSettings()
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

