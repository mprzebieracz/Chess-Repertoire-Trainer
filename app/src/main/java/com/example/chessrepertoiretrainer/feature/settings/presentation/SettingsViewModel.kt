package com.example.chessrepertoiretrainer.feature.settings.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.chessrepertoiretrainer.feature.settings.data.AppThemeMode
import com.example.chessrepertoiretrainer.feature.settings.data.BoardTheme
import com.example.chessrepertoiretrainer.feature.settings.data.UserSettings
import com.example.chessrepertoiretrainer.feature.settings.data.UserSettingsRepository
import com.example.chessrepertoiretrainer.feature.settings.domain.usecase.UpdateSettingUseCase
import com.example.chessrepertoiretrainer.feature.settings.presentation.state.SettingsScreenState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: UserSettingsRepository,
    private val updateSettingUseCase: UpdateSettingUseCase
) : ViewModel() {

    val settings: StateFlow<UserSettings> = repository.settingsFlow.stateIn(
        scope = viewModelScope, 
        started = SharingStarted.WhileSubscribed(5_000), 
        initialValue = UserSettings()
    )

    private val _screenState = MutableStateFlow(SettingsScreenState())
    val screenState: StateFlow<SettingsScreenState> = _screenState.asStateFlow()

    fun updateLichessUsername(username: String) {
        viewModelScope.launch {
            _screenState.value = _screenState.value.copy(isSaving = true, saveErrorMessage = null, saveSuccessMessage = null)
            updateSettingUseCase.lichessUsername(username)
                .onSuccess {
                    _screenState.value = _screenState.value.copy(isSaving = false, saveSuccessMessage = "Saved")
                    Log.d("SettingsViewModel", "Lichess username updated: $username")
                }
                .onFailure { error ->
                    _screenState.value = _screenState.value.copy(isSaving = false, saveErrorMessage = error.message ?: "Save failed")
                    Log.e("SettingsViewModel", "Failed to update Lichess username", error)
                }
        }
    }

    fun updateChessComUsername(username: String) {
        viewModelScope.launch {
            _screenState.value = _screenState.value.copy(isSaving = true, saveErrorMessage = null, saveSuccessMessage = null)
            updateSettingUseCase.chessComUsername(username)
                .onSuccess {
                    _screenState.value = _screenState.value.copy(isSaving = false, saveSuccessMessage = "Saved")
                    Log.d("SettingsViewModel", "Chess.com username updated: $username")
                }
                .onFailure { error ->
                    _screenState.value = _screenState.value.copy(isSaving = false, saveErrorMessage = error.message ?: "Save failed")
                    Log.e("SettingsViewModel", "Failed to update Chess.com username", error)
                }
        }
    }

    fun clearTransientMessages() {
        _screenState.value = _screenState.value.copy(saveSuccessMessage = null, saveErrorMessage = null)
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
        private val repository: UserSettingsRepository,
        private val updateSettingUseCase: UpdateSettingUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            return SettingsViewModel(repository, updateSettingUseCase) as T
        }
    }
}