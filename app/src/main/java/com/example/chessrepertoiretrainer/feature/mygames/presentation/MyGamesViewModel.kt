package com.example.chessrepertoiretrainer.feature.mygames.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.chessrepertoiretrainer.feature.mygames.data.GameSyncManager
import com.example.chessrepertoiretrainer.feature.settings.data.UserSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class MyGamesUiState(
    val lichessUsername: String = "",
    val chessComUsername: String = "",
    val isSyncing: Boolean = false,
    val syncProgress: String? = null,
    val syncError: String? = null
)

class MyGamesViewModel(
    private val syncManager: GameSyncManager,
    private val settingsRepository: UserSettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyGamesUiState())
    val uiState: StateFlow<MyGamesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val settings = settingsRepository.settingsFlow.first()
            _uiState.value = _uiState.value.copy(
                lichessUsername = settings.lichessUsername,
                chessComUsername = settings.chessComUsername
            )
        }
    }

    fun sync() {
        if (_uiState.value.isSyncing) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSyncing = true, syncProgress = "Starting sync…", syncError = null
            )
            try {
                syncManager.syncAll { platform, count ->
                    _uiState.value = _uiState.value.copy(
                        syncProgress = "${platform.replaceFirstChar { it.uppercase() }}: $count fetched…"
                    )
                }
                _uiState.value = _uiState.value.copy(isSyncing = false, syncProgress = null)
            }
            catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSyncing = false, syncProgress = null,
                    syncError = e.message ?: "Sync failed"
                )
            }
        }
    }

    class Factory(
        private val syncManager: GameSyncManager,
        private val settingsRepository: UserSettingsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            return MyGamesViewModel(syncManager, settingsRepository) as T
        }
    }
}
