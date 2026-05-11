package com.example.chessrepertoiretrainer.feature.mygames.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.chessrepertoiretrainer.feature.mygames.data.GameSyncManager
import com.example.chessrepertoiretrainer.feature.mygames.data.SavedGameRepository
import com.example.chessrepertoiretrainer.feature.settings.data.UserSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class MyGamesUiState(val lichessUsername: String = "",
                          val chessComUsername: String = "",
                          val lichessGameCount: Int = 0,
                          val chessComGameCount: Int = 0,
                          val lichessLastSyncAt: Long = 0L,
                          val chessComLastSyncAt: Long = 0L,
                          val isSyncing: Boolean = false,
                          val syncProgress: String? = null,
                          val syncError: String? = null)

class MyGamesViewModel(private val syncManager: GameSyncManager,
                       private val settingsRepository: UserSettingsRepository,
                       private val savedGameRepository: SavedGameRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(MyGamesUiState())
    val uiState: StateFlow<MyGamesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { settings ->
                val lichessCount =
                    if (settings.lichessUsername.isNotBlank()) savedGameRepository.countGames("lichess",
                                                                                              settings.lichessUsername)
                    else 0
                val chessComCount =
                    if (settings.chessComUsername.isNotBlank()) savedGameRepository.countGames("chess.com",
                                                                                               settings.chessComUsername)
                    else 0
                _uiState.value = _uiState.value.copy(lichessUsername = settings.lichessUsername,
                                                     chessComUsername = settings.chessComUsername,
                                                     lichessGameCount = lichessCount,
                                                     chessComGameCount = chessComCount,
                                                     lichessLastSyncAt = settings.lichessLastSyncAt,
                                                     chessComLastSyncAt = settings.chessComLastSyncAt)
            }
        }
    }

    fun sync() {
        if (_uiState.value.isSyncing) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true,
                                                 syncProgress = "Starting sync…",
                                                 syncError = null)
            try {
                syncManager.syncAll { platform, count ->
                    _uiState.value =
                        _uiState.value.copy(syncProgress = "${platform.replaceFirstChar { it.uppercase() }}: $count fetched…")
                }
                val now = System.currentTimeMillis()
                val settings = settingsRepository.settingsFlow.first()
                if (settings.lichessUsername.isNotBlank()) {
                    settingsRepository.updateLichessLastSyncAt(now)
                }
                if (settings.chessComUsername.isNotBlank()) {
                    settingsRepository.updateChessComLastSyncAt(now)
                }
                val lichessCount =
                    if (settings.lichessUsername.isNotBlank()) savedGameRepository.countGames("lichess",
                                                                                              settings.lichessUsername)
                    else _uiState.value.lichessGameCount
                val chessComCount =
                    if (settings.chessComUsername.isNotBlank()) savedGameRepository.countGames("chess.com",
                                                                                               settings.chessComUsername)
                    else _uiState.value.chessComGameCount
                _uiState.value = _uiState.value.copy(isSyncing = false,
                                                     syncProgress = null,
                                                     lichessGameCount = lichessCount,
                                                     chessComGameCount = chessComCount,
                                                     lichessLastSyncAt = if (settings.lichessUsername.isNotBlank()) now else _uiState.value.lichessLastSyncAt,
                                                     chessComLastSyncAt = if (settings.chessComUsername.isNotBlank()) now else _uiState.value.chessComLastSyncAt)
            }
            catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSyncing = false,
                                                     syncProgress = null,
                                                     syncError = e.message ?: "Sync failed")
            }
        }
    }

    class Factory(private val syncManager: GameSyncManager,
                  private val settingsRepository: UserSettingsRepository,
                  private val savedGameRepository: SavedGameRepository) :
        ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            return MyGamesViewModel(syncManager, settingsRepository, savedGameRepository) as T
        }
    }
}