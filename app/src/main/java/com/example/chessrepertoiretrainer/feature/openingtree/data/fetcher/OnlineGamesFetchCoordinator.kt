package com.example.chessrepertoiretrainer.feature.openingtree.data.fetcher

class OnlineGamesFetchCoordinator(
    private val fetcherRegistry: GameFetcherRegistry
) {

    data class FetchResult(
        val games: List<FetchedGame>, val errorMessage: String? = null
    )

    suspend fun fetchGames(username: String, platform: String): FetchResult {
        val normalizedUsername = username.trim()

        if (normalizedUsername.isBlank()) {
            return FetchResult(emptyList(), "Username cannot be blank")
        }

        val fetcher = fetcherRegistry.getFetcher(platform) ?: return FetchResult(
            emptyList(),
            "Unsupported platform: $platform"
        )

        return runCatching {
            fetcher.fetchGamesForUser(
                username = normalizedUsername, since = null, maxGames = null
            )
        }.fold(onSuccess = { FetchResult(games = it) }, onFailure = {
            FetchResult(
                games = emptyList(),
                errorMessage = it.message ?: "Unexpected error while fetching games"
            )
        })
    }
}