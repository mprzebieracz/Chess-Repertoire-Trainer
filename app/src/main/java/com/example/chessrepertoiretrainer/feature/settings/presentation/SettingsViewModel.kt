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
import com.example.chessrepertoiretrainer.feature.settings.presentation.state.SettingsUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: UserSettingsRepository,
                        private val updateSettingUseCase: UpdateSettingUseCase) : ViewModel() {

    val settings: StateFlow<UserSettings> = repository.settingsFlow.stateIn(scope = viewModelScope,
                                                                            started = SharingStarted.WhileSubscribed(
                                                                                5_000),
                                                                            initialValue = UserSettings())

    private val _screenState = MutableStateFlow(SettingsUiState())
    val screenState: StateFlow<SettingsUiState> = _screenState.asStateFlow()

    fun updateLichessUsername(username: String) {
        saveUsername(username = username,
                     onSave = updateSettingUseCase::lichessUsername,
                     successLogMessage = "Lichess username updated: $username",
                     failureLogMessage = "Failed to update Lichess username")
    }

    fun updateChessComUsername(username: String) {
        saveUsername(username = username,
                     onSave = updateSettingUseCase::chessComUsername,
                     successLogMessage = "Chess.com username updated: $username",
                     failureLogMessage = "Failed to update Chess.com username")
    }

    fun clearTransientMessages() {
        _screenState.value =
            _screenState.value.copy(saveSuccessMessage = null, saveErrorMessage = null)
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

    fun updateEngineDepth(depth: Int) {
        viewModelScope.launch { repository.updateEngineDepth(depth) }
    }

    fun updateEngineMovetime(ms: Int) {
        viewModelScope.launch { repository.updateEngineMovetime(ms) }
    }

    fun updateEngineThreads(threads: Int) {
        viewModelScope.launch { repository.updateEngineThreads(threads) }
    }

    private fun saveUsername(username: String,
                             onSave: suspend (String) -> Result<Unit>,
                             successLogMessage: String,
                             failureLogMessage: String) {
        viewModelScope.launch {
            setSavingState()
            onSave(username).onSuccess {
                _screenState.value =
                    _screenState.value.copy(isSaving = false, saveSuccessMessage = "Saved")
                Log.d("SettingsViewModel", successLogMessage)
            }.onFailure { error ->
                _screenState.value = _screenState.value.copy(isSaving = false,
                                                             saveErrorMessage = error.message
                                                                 ?: "Save failed")
                Log.e("SettingsViewModel", failureLogMessage, error)
            }
        }
    }

    private fun setSavingState() {
        _screenState.value = _screenState.value.copy(isSaving = true,
                                                     saveErrorMessage = null,
                                                     saveSuccessMessage = null)
    }

    class Factory(private val repository: UserSettingsRepository,
                  private val updateSettingUseCase: UpdateSettingUseCase) :
        ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            return SettingsViewModel(repository, updateSettingUseCase) as T
        }
    }
}