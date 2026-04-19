package com.example.chessrepertoiretrainer.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.chessrepertoiretrainer.data.AccountSyncCoordinator
import com.example.chessrepertoiretrainer.data.GameWithPgn
import com.example.chessrepertoiretrainer.data.OpeningTreeCache
import com.example.chessrepertoiretrainer.data.OpeningTreePreparationCoordinator
import com.example.chessrepertoiretrainer.data.StatsRefreshCoordinator
import com.example.chessrepertoiretrainer.domain.games.GamesRepository
import com.example.chessrepertoiretrainer.domain.stats.GameStatsSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the "My stats" screen. It works with the persisted
 * player profiles and [GamesRepository] to keep the user's own games stored
 * locally and to eagerly build opening trees for their accounts.
 */
 class MyStatsViewModel(
     private val accountSyncCoordinator: AccountSyncCoordinator,
     private val statsRefreshCoordinator: StatsRefreshCoordinator,
     private val openingTreePreparationCoordinator: OpeningTreePreparationCoordinator
 ) : ViewModel() {

    private val _uiState = MutableStateFlow(MyStatsUiState())
    val uiState: StateFlow<MyStatsUiState> = _uiState.asStateFlow()

    /**
     * Synthetic id used for the combined "My stats" opening trees built from
     * one or more platforms. This id never collides with real PlayerProfile
     * ids from the database and is only used as a cache key for
     * [OpeningTreeCache].
     */
    private companion object {
        private const val COMBINED_PROFILE_ID: Long = -1000L
    }

    /**
     * Should be called whenever the stored usernames change so that we can
     * ensure matching profiles exist and pre-populate basic stats.
     */
    fun onUserSettingsChanged(lichessUsername: String, chessComUsername: String) {
        viewModelScope.launch {
            handleUsernameUpdate(platform = "lichess", username = lichessUsername)
            handleUsernameUpdate(platform = "chess.com", username = chessComUsername)

            val current = _uiState.value
            _uiState.value = current.copy(
                isAnySyncing = (current.lichess?.isSyncing == true || current.chessCom?.isSyncing == true)
            )
        }
    }

    private suspend fun handleUsernameUpdate(platform: String, username: String) {
        val trimmed = username.trim()
        if (trimmed.isEmpty()) {
            updateAccount(platform) { null }
            return
        }

        val profile = accountSyncCoordinator.resolveOrCreateProfile(trimmed, platform)
            ?: return

        val gamesCount = accountSyncCoordinator.getStoredGamesCount(profile.id)

        updateAccount(platform) { existing ->
            val base = existing ?: AccountStats(
                profileId = profile.id,
                username = profile.username,
                platform = profile.platform
            )
            base.copy(
                profileId = profile.id,
                username = profile.username,
                platform = profile.platform,
                gamesCount = gamesCount,
                lastSyncTime = profile.lastSyncTime
            )
        }

        // Precompute simple aggregated stats for this account so that the
        // "My stats" screen can show a quick overview without having to
        // parse PGNs on every recomposition. This runs off the main thread.
        if (gamesCount > 0) {
            val summary = statsRefreshCoordinator.recomputeAndLoadSummary(profile.id)
            if (summary != null) {
                val current = _uiState.value
                _uiState.value = when (platform.lowercase()) {
                    "lichess" -> current.copy(lichessStats = summary)
                    "chess.com" -> current.copy(chessComStats = summary)
                    else -> current
                }
            }
        }
    }


    /**
     * Build an opening tree from one or both configured platforms (Lichess
     * and/or Chess.com) using a single set of filters. Games are first
     * synchronized and stored locally, then combined into a single in-memory
     * tree cached under a synthetic profile id.
     */
    fun syncAndPrepareTreeForSelection(
        useLichess: Boolean,
        useChessCom: Boolean,
        color: String,
        timeControlFilter: String,
        maxGamesForTree: Int?,
        onProfileReady: (Long) -> Unit
    ) {
        viewModelScope.launch {
            val current = _uiState.value
            val accountsToUse = mutableListOf<AccountStats>()
            if (useLichess) current.lichess?.let { accountsToUse.add(it) }
            if (useChessCom) current.chessCom?.let { accountsToUse.add(it) }

            if (accountsToUse.isEmpty()) return@launch

            // Fast path: if we already have a cached combined tree for these
            // filters and no new sync is required, reuse it immediately.
            val cacheKey = openingTreePreparationCoordinator.buildCacheKey(
                profileId = COMBINED_PROFILE_ID,
                color = color,
                timeControlFilter = timeControlFilter,
                maxGamesForTree = maxGamesForTree
            )
            val cached = OpeningTreeCache.get(cacheKey)
            if (cached != null) {
                onProfileReady(COMBINED_PROFILE_ID)
                return@launch
            }

            // Mark selected accounts as syncing/building.
            accountsToUse.forEach { acc ->
                    updateAccount(acc.platform) { existing ->
                    val base = existing ?: acc
                    base.copy(
                        isSyncing = true,
                        statusMessage = "Preparing games...",
                        errorMessage = null,
                        lastSyncSummary = null
                    )
                }
            }
            markGlobalSyncing()

            // Synchronize games only once per account: if we already have
            // some games stored (gamesCount > 0), skip remote sync to keep
            // this operation fast. The user can always re-sync elsewhere in
            // the app in the future if needed.
            accountsToUse.forEach { acc ->
                if (acc.gamesCount > 0) return@forEach

                val sync = accountSyncCoordinator.syncAccount(acc.profileId)
                updateAccount(acc.platform) { existing ->
                    val base = existing ?: acc
                    base.copy(
                        lastSyncTime = sync.updatedLastSyncTime ?: base.lastSyncTime,
                        errorMessage = sync.errorMessage
                    )
                }
            }

            // Gather (optionally limited) games for all selected accounts.
            val allGamesWithPgn = mutableListOf<GameWithPgn>()
            accountsToUse.forEach { acc ->
                val gamesForProfile = accountSyncCoordinator.getGamesWithPgnForProfile(
                    profileId = acc.profileId,
                    maxGames = maxGamesForTree
                )
                allGamesWithPgn += gamesForProfile

                // Keep basic stats up-to-date.
                updateAccount(acc.platform) { existing ->
                    val base = existing ?: acc
                    base.copy(gamesCount = gamesForProfile.size)
                }
            }

            val gamesUsedForTree = try {
                openingTreePreparationCoordinator.prepareFromStoredGames(
                    profileId = COMBINED_PROFILE_ID,
                    games = allGamesWithPgn,
                    color = color,
                    timeControlFilter = timeControlFilter,
                    maxGamesForTree = maxGamesForTree
                )
            } catch (_: Exception) {
                0
            }

            // Clear syncing flags and surface a summary across accounts.
            accountsToUse.forEach { acc ->
                updateAccount(acc.platform) { existing ->
                    val base = existing ?: acc
                    base.copy(
                        isSyncing = false,
                        statusMessage = null,
                        lastSyncSummary = if (gamesUsedForTree > 0) {
                            "Prepared opening tree from $gamesUsedForTree games"
                        } else {
                            base.lastSyncSummary
                        }
                    )
                }
            }
            markGlobalSyncing()

            // After preparing trees (which may have performed an initial
            // sync for some accounts), recompute per-account stats so the UI
            // can immediately show updated numbers.
            accountsToUse.forEach { acc ->
                recomputeAndApplySummaryForAccount(acc)
            }

            if (gamesUsedForTree > 0) {
                onProfileReady(COMBINED_PROFILE_ID)
            }
        }
    }

    /**
     * Explicitly refresh games from the remote APIs for the selected
     * platforms. This will contact Lichess and/or Chess.com, fetch any games
     * that are newer than the profile's last sync time, and store them in the
     * local database. It also clears any cached opening trees for the
     * combined "My stats" view so that subsequent tree builds include the
     * newly downloaded games.
     */
    fun refreshGamesForSelection(
        useLichess: Boolean,
        useChessCom: Boolean
    ) {
        viewModelScope.launch {
            val current = _uiState.value
            val accountsToUse = mutableListOf<AccountStats>()
            if (useLichess) current.lichess?.let { accountsToUse.add(it) }
            if (useChessCom) current.chessCom?.let { accountsToUse.add(it) }

            if (accountsToUse.isEmpty()) return@launch

            // Mark selected accounts as syncing.
            accountsToUse.forEach { acc ->
                updateAccount(acc.platform) { existing ->
                    val base = existing ?: acc
                    base.copy(
                        isSyncing = true,
                        statusMessage = "Checking for new games...",
                        errorMessage = null,
                        lastSyncSummary = null
                    )
                }
            }
            markGlobalSyncing()

            // For each selected account, perform an incremental sync based on
            // lastSyncTime. The underlying repository/fetcher is responsible
            // for calling the remote API and returning only games newer than
            // the last sync.
            accountsToUse.forEach { acc ->
                val sync = accountSyncCoordinator.syncAccount(acc.profileId)

                updateAccount(acc.platform) { existing ->
                    val base = existing ?: acc
                    base.copy(
                        isSyncing = false,
                        statusMessage = null,
                        lastSyncTime = sync.updatedLastSyncTime ?: base.lastSyncTime,
                        gamesCount = if (sync.gamesCount > 0) sync.gamesCount else base.gamesCount,
                        lastSyncSummary = if (sync.errorMessage == null) {
                            if (sync.newGames > 0) {
                                "Downloaded ${sync.newGames} new games"
                            } else {
                                "No new games found"
                            }
                        } else {
                            base.lastSyncSummary
                        },
                        errorMessage = sync.errorMessage
                    )
                }
            }

            // Clear any cached trees that might now be stale for this
            // combined view.
            OpeningTreeCache.clearForProfile(COMBINED_PROFILE_ID)
            accountsToUse.forEach { acc ->
                OpeningTreeCache.clearForProfile(acc.profileId)
            }

            markGlobalSyncing()

            // After syncing games, recompute per-account stats so the UI can
            // immediately show updated numbers.
            accountsToUse.forEach { acc ->
                recomputeAndApplySummaryForAccount(acc)
            }
        }
    }

    private suspend fun recomputeAndApplySummaryForAccount(account: AccountStats) {
        val summary = statsRefreshCoordinator.recomputeAndLoadSummary(account.profileId)
        if (summary != null) {
            val current = _uiState.value
            _uiState.value = when (account.platform.lowercase()) {
                "lichess" -> current.copy(lichessStats = summary)
                "chess.com" -> current.copy(chessComStats = summary)
                else -> current
            }
        }
    }

    // Filtering logic is delegated to OpeningTreeFilterUtils to keep
    // behavior consistent with OpeningTreeViewModel.

    fun clearError(platform: String) {
        val normalized = platform.trim().lowercase()
        updateAccount(normalized) { current ->
            current?.copy(errorMessage = null)
        }
    }

    private fun updateAccount(platform: String, block: (AccountStats?) -> AccountStats?) {
        val normalized = platform.trim().lowercase()
        val current = _uiState.value
        val updated = when (normalized) {
            "lichess" -> current.copy(lichess = block(current.lichess))
            "chess.com" -> current.copy(chessCom = block(current.chessCom))
            else -> current
        }
        _uiState.value = updated.copy(
            isAnySyncing = (updated.lichess?.isSyncing == true || updated.chessCom?.isSyncing == true)
        )
    }

    private fun markGlobalSyncing() {
        val current = _uiState.value
        _uiState.value = current.copy(
            isAnySyncing = (current.lichess?.isSyncing == true || current.chessCom?.isSyncing == true)
        )
    }

    data class MyStatsUiState(
        val lichess: AccountStats? = null,
        val chessCom: AccountStats? = null,
        val isAnySyncing: Boolean = false,
        val lichessStats: GameStatsSummary? = null,
        val chessComStats: GameStatsSummary? = null
    )

    data class AccountStats(
        val profileId: Long,
        val username: String,
        val platform: String,
        val gamesCount: Int = 0,
        val lastSyncTime: Long? = null,
        val isSyncing: Boolean = false,
        val statusMessage: String? = null,
        val lastSyncSummary: String? = null,
        val errorMessage: String? = null
    )

    class Factory(
        private val accountSyncCoordinator: AccountSyncCoordinator,
        private val statsRefreshCoordinator: StatsRefreshCoordinator,
        private val openingTreePreparationCoordinator: OpeningTreePreparationCoordinator
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            return MyStatsViewModel(
                accountSyncCoordinator = accountSyncCoordinator,
                statsRefreshCoordinator = statsRefreshCoordinator,
                openingTreePreparationCoordinator = openingTreePreparationCoordinator
            ) as T
        }
    }
}

