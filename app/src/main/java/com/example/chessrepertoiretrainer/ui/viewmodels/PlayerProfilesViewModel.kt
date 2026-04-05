package com.example.chessrepertoiretrainer.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.chessrepertoiretrainer.data.GameForOpeningTree
import com.example.chessrepertoiretrainer.data.OpeningTreeBuilder
import com.example.chessrepertoiretrainer.data.OpeningTreeCache
import com.example.chessrepertoiretrainer.data.OpeningTreeCacheKey
import com.example.chessrepertoiretrainer.data.PlayerProfileRepository
import com.example.chessrepertoiretrainer.database.entities.PlayerProfile
import com.example.chessrepertoiretrainer.domain.games.GamesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel for managing tracked player profiles used for opening-tree
 * analysis.
 */
 class PlayerProfilesViewModel(
    private val repository: PlayerProfileRepository,
    private val gamesRepository: GamesRepository
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
     * Convenience helper used by the "Your games" (Opening tree setup) screen:
     * ensure there is a profile for the given username+platform, download
     * games for it, eagerly build the opening tree with the chosen filters,
     * and then invoke [onProfileReady] with the profile id so the UI can open
     * the opening-tree screen which simply displays the prepared tree.
     */
    fun syncGamesForUsername(
        username: String,
        platform: String,
        maxGamesForTree: Int?,
        color: String,
        timeControlFilter: String,
        onProfileReady: (Long) -> Unit
    ) {
        if (username.isBlank() || _uiState.value.isSyncing) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSyncing = true,
                errorMessage = null,
                lastSyncSummary = null,
                statusMessage = "Fetching games..."
            )

            val profile = try {
                repository.getOrCreateProfile(username, platform)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    errorMessage = e.message ?: "Unexpected error while preparing profile",
                    statusMessage = null
                )
                return@launch
            }

              val result = try {
                // Download/sync games for this profile without applying the
                // user-facing maxGames filter here. The maxGames value is used
                // later when building the opening tree, *after* applying
                // color/time-control filters.
                gamesRepository.syncGamesForProfile(profile.id, null)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    errorMessage = e.message ?: "Unexpected error during sync",
                    statusMessage = null
                )
                return@launch
            }

            // Surface any sync error but still attempt to build the tree from
            // whatever games are available locally.
            _uiState.value = _uiState.value.copy(
                errorMessage = result.errorMessage
            )

            // Even if there was a non-fatal error message, attempt to build
            // the opening tree from whatever games are currently stored.
            val gamesUsedForTree = try {
                buildAndCacheOpeningTreeForProfile(
                    profileId = profile.id,
                    color = color,
                    timeControlFilter = timeControlFilter,
                    maxGamesForTree = maxGamesForTree
                )
            } catch (_: Exception) {
                0
            }

            _uiState.value = _uiState.value.copy(
                isSyncing = false,
                statusMessage = null,
                lastSyncSummary = if (gamesUsedForTree > 0) {
                    "Prepared opening tree from $gamesUsedForTree games"
                } else {
                    null
                }
            )

            onProfileReady(profile.id)
        }
    }

     /**
      * Build an opening tree for the given profile and filters and store it in
      * the in-memory [OpeningTreeCache] so that the dedicated opening-tree
      * screen can display it immediately.
      */
       private suspend fun buildAndCacheOpeningTreeForProfile(
           profileId: Long,
           color: String,
           timeControlFilter: String,
           maxGamesForTree: Int?
       ): Int {
         val normalizedColor = color.trim().lowercase().ifEmpty { "both" }
         val normalizedTimeControl = timeControlFilter.trim()

         // Step 1: load all games with PGN.
         _uiState.value = _uiState.value.copy(statusMessage = "Preparing games...")
         val allGamesWithPgn = gamesRepository.getGamesWithPgnForProfile(profileId)
         if (allGamesWithPgn.isEmpty()) {
             // Nothing to build; clear any existing cached trees for this
             // profile to avoid showing stale data.
             OpeningTreeCache.clearForProfile(profileId)
             _uiState.value = _uiState.value.copy(statusMessage = "No games stored for this profile yet.")
             return 0
         }

         // Step 2: apply filters on the main thread (cheap operations).
         _uiState.value = _uiState.value.copy(statusMessage = "Applying filters...")

         val colorFilterEnum = when (normalizedColor) {
             "white" -> OpeningTreeViewModel.ColorFilter.WHITE_ONLY
             "black" -> OpeningTreeViewModel.ColorFilter.BLACK_ONLY
             else -> OpeningTreeViewModel.ColorFilter.BOTH
         }

         val filteredGames = OpeningTreeFilterUtils.filterGames(
             games = allGamesWithPgn,
             colorFilter = colorFilterEnum,
             timeControlFilter = normalizedTimeControl
         )

         if (filteredGames.isEmpty()) {
             OpeningTreeCache.clearForProfile(profileId)
             _uiState.value = _uiState.value.copy(statusMessage = "No games match current filters.")
             return 0
         }

         // Step 3: apply max-games limit, if any.
         val limitedGames = maxGamesForTree?.let { limit ->
             _uiState.value = _uiState.value.copy(statusMessage = "Limiting to $limit games...")
             filteredGames.take(limit)
         } ?: filteredGames

         // Step 4: build the tree off the main thread.
         _uiState.value = _uiState.value.copy(statusMessage = "Building opening tree...")

         val tree = withContext(Dispatchers.Default) {
             if (limitedGames.isEmpty()) {
                 null
             } else {
                 val gamesForTree = limitedGames.map { gwp ->
                     GameForOpeningTree(
                         pgn = gwp.pgn,
                         isUserWhite = gwp.game.isUserWhite,
                         resultTag = gwp.game.result
                     )
                 }
                 OpeningTreeBuilder.buildTree(gamesForTree)
             }
         }

         // Refresh cache entries for this profile.
         OpeningTreeCache.clearForProfile(profileId)

         if (tree != null) {
             val cacheKey = OpeningTreeCacheKey(
                 profileId = profileId,
                 color = normalizedColor,
                 timeControlFilter = normalizedTimeControl,
                 maxGamesForTree = maxGamesForTree
             )
             OpeningTreeCache.put(cacheKey, tree)
             _uiState.value = _uiState.value.copy(statusMessage = "Opening tree ready.")
             return limitedGames.size
         }

         return 0
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
                lastSyncSummary = null,
                statusMessage = "Fetching games..."
            )

            val result = try {
                gamesRepository.syncGamesForProfile(profileId, maxGames)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    errorMessage = e.message ?: "Unexpected error during sync",
                    statusMessage = null
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
                },
                statusMessage = null
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    data class PlayerProfilesUiState(
        val errorMessage: String? = null,
        val isSyncing: Boolean = false,
        val lastSyncSummary: String? = null,
        val statusMessage: String? = null
     )

    class Factory(
        private val repository: PlayerProfileRepository,
        private val gamesRepository: GamesRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            return PlayerProfilesViewModel(repository, gamesRepository) as T
        }
    }
}

