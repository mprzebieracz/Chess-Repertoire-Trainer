package com.example.chessrepertoiretrainer.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.chessrepertoiretrainer.data.PlayerGamesRepository
import com.example.chessrepertoiretrainer.data.PlayerProfileRepository
import com.example.chessrepertoiretrainer.database.entities.PlayerProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for managing tracked player profiles used for opening-tree
 * analysis.
 */
 class PlayerProfilesViewModel(
    private val repository: PlayerProfileRepository,
    private val gamesRepository: PlayerGamesRepository
 ) : ViewModel() {

    val profiles: StateFlow<List<PlayerProfile>> =
        repository.getAllProfiles()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    private val _uiState = MutableStateFlow(PlayerProfilesUiState())
    val uiState: StateFlow<PlayerProfilesUiState> = _uiState.asStateFlow()

    fun addProfile(username: String, platform: String) {
        if (username.isBlank()) return
        viewModelScope.launch {
            try {
                repository.addOrUpdateProfile(username, platform)
                _uiState.value = PlayerProfilesUiState()
            } catch (e: Exception) {
                _uiState.value = PlayerProfilesUiState(errorMessage = e.message)
            }
        }
    }

    /**
     * Convenience helper used by the "Your games" screen: ensure there is a
     * profile for the given username+platform, download games for it, and then
     * invoke [onProfileReady] with the profile id so the UI can open the
     * opening-tree screen.
     */
    fun syncGamesForUsername(
        username: String,
        platform: String,
        maxGames: Int?,
        onProfileReady: (Long) -> Unit
    ) {
        if (username.isBlank() || _uiState.value.isSyncing) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSyncing = true,
                errorMessage = null,
                lastSyncSummary = null
            )

            val profile = try {
                repository.getOrCreateProfile(username, platform)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    errorMessage = e.message ?: "Unexpected error while preparing profile"
                )
                return@launch
            }

            val result = try {
                gamesRepository.syncGamesForProfile(profile.id, maxGames)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    errorMessage = e.message ?: "Unexpected error during sync"
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(
                isSyncing = false,
                errorMessage = result.errorMessage,
                lastSyncSummary = if (result.errorMessage == null) {
                    "Downloaded ${result.newGames} new games"
                } else {
                    null
                }
            )

            onProfileReady(profile.id)
        }
    }

    /**
     * Trigger download/synchronization of games for a given player profile.
     */
    fun syncGamesForProfile(profileId: Long, maxGames: Int? = null) {
        if (_uiState.value.isSyncing) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSyncing = true,
                errorMessage = null,
                lastSyncSummary = null
            )

            val result = try {
                gamesRepository.syncGamesForProfile(profileId, maxGames)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    errorMessage = e.message ?: "Unexpected error during sync"
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(
                isSyncing = false,
                errorMessage = result.errorMessage,
                lastSyncSummary = if (result.errorMessage == null) {
                    "Downloaded ${result.newGames} new games"
                } else {
                    null
                }
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    data class PlayerProfilesUiState(
        val errorMessage: String? = null,
        val isSyncing: Boolean = false,
        val lastSyncSummary: String? = null
    )

    class Factory(
        private val repository: PlayerProfileRepository,
        private val gamesRepository: PlayerGamesRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            return PlayerProfilesViewModel(repository, gamesRepository) as T
        }
    }
}

