package com.example.chessrepertoiretrainer.feature.mygames.data

import com.example.chessrepertoiretrainer.feature.openingtree.data.fetcher.GameFetcherRegistry
import com.example.chessrepertoiretrainer.feature.settings.data.UserSettingsRepository
import kotlinx.coroutines.flow.first

class GameSyncManager(private val fetcherRegistry: GameFetcherRegistry,
                      private val repository: SavedGameRepository,
                      private val settingsRepository: UserSettingsRepository) {
    suspend fun syncAll(onProgress: (platform: String, count: Int) -> Unit): Map<String, Int> {
        val settings = settingsRepository.settingsFlow.first()
        val results = mutableMapOf<String, Int>()

        if (settings.lichessUsername.isNotBlank()) {
            results["lichess"] = syncAccount(username = settings.lichessUsername,
                                             platform = "lichess",
                                             onProgress = { onProgress("lichess", it) })
        }

        if (settings.chessComUsername.isNotBlank()) {
            results["chess.com"] = syncAccount(username = settings.chessComUsername,
                                               platform = "chess.com",
                                               onProgress = { onProgress("chess.com", it) })
        }

        return results
    }

    suspend fun syncAccount(username: String, platform: String, onProgress: (Int) -> Unit): Int {
        val fetcher = fetcherRegistry.getFetcher(platform) ?: return 0
        val since = repository.getLatestPlayedAt(platform, username)

        val fetched =
            fetcher.fetchGamesForUser(username = username, since = since, onProgress = onProgress)

        val toInsert = fetched.map { it.toSavedGame(platform, username) }
        repository.insertGames(toInsert)
        return toInsert.size
    }
}