package com.example.chessrepertoiretrainer.data

/**
 * Fetches online games for opening-tree search flows.
 */
class OnlineGamesFetchCoordinator(
    private val fetcherRegistry: GameFetcherRegistry
) {

    data class FetchResult(
        val games: List<FetchedGame>,
        val errorMessage: String? = null
    )

    suspend fun fetchGames(
        username: String,
        platform: String
    ): FetchResult {
        val normalizedUsername = username.trim()
        val normalizedPlatform = platform.trim().lowercase()

        if (normalizedUsername.isBlank()) {
            return FetchResult(emptyList(), "Username cannot be blank")
        }

        val fetcher = fetcherRegistry.getFetcher(normalizedPlatform)
            ?: return FetchResult(emptyList(), "Unsupported platform: $platform")

        return try {
            val games = fetcher.fetchGamesForUser(
                username = normalizedUsername,
                since = null,
                maxGames = null
            )
            FetchResult(games = games)
        } catch (e: Exception) {
            FetchResult(
                games = emptyList(),
                errorMessage = e.message ?: "Unexpected error while fetching games"
            )
        }
    }
}

