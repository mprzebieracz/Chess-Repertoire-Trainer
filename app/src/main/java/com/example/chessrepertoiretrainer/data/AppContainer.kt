package com.example.chessrepertoiretrainer.data

import android.content.Context
import com.example.chessrepertoiretrainer.database.ChessDatabase
import com.example.chessrepertoiretrainer.domain.games.GamesRepository

/**
 * Simple application-level dependency container.
 *
 * Keeps heavyweight singletons (database, repositories, fetcher registry)
 * out of composables so they are not recreated during recomposition.
 */
class AppContainer(context: Context) {

    private val db: ChessDatabase = ChessDatabase.getDatabase(context)

    val repertoireDao = db.repertoireDao()
    val puzzleDao = db.puzzleDao()
    val playerProfileDao = db.playerProfileDao()
    val gameDao = db.gameDao()
    val gameStatsDao = db.gameStatsDao()

    val puzzleRepository = DefaultPuzzleRepository(puzzleDao)
    val playerProfileRepository = PlayerProfileRepository(playerProfileDao)

    val gameFetcherRegistry = GameFetcherRegistry(
        listOf(
            LichessGameFetcher,
            ChessComGameFetcher
        )
    )

    val openingTreePreparationCoordinator = OpeningTreePreparationCoordinator()
    val onlineGamesFetchCoordinator = OnlineGamesFetchCoordinator(gameFetcherRegistry)

    val gamesRepository: GamesRepository = PlayerGamesRepository(
        gameDao = gameDao,
        playerProfileDao = playerProfileDao,
        fetcherRegistry = gameFetcherRegistry
    )

    val gameStatsRepository = DefaultGameStatsRepository(
        gamesRepository = gamesRepository,
        gameStatsDao = gameStatsDao
    )

    val accountSyncCoordinator = AccountSyncCoordinator(
        profileRepository = playerProfileRepository,
        gamesRepository = gamesRepository
    )

    val statsRefreshCoordinator = StatsRefreshCoordinator(
        gameStatsRepository = gameStatsRepository
    )
}

