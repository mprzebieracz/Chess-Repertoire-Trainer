package com.example.chessrepertoiretrainer.app

import android.content.Context
import com.example.chessrepertoiretrainer.core.database.ChessDatabase
import com.example.chessrepertoiretrainer.feature.openingtree.data.ChessComGameFetcher
import com.example.chessrepertoiretrainer.feature.openingtree.data.GameFetcherRegistry
import com.example.chessrepertoiretrainer.feature.openingtree.data.LichessGameFetcher
import com.example.chessrepertoiretrainer.feature.openingtree.data.OnlineGamesFetchCoordinator
import com.example.chessrepertoiretrainer.feature.openingtree.data.OpeningTreePreparationCoordinator
import com.example.chessrepertoiretrainer.feature.openingtree.data.PlayerGamesRepository
import com.example.chessrepertoiretrainer.feature.openingtree.domain.GamesRepository
import com.example.chessrepertoiretrainer.feature.puzzles.data.DefaultPuzzleRepository
import com.example.chessrepertoiretrainer.feature.settings.data.UserSettingsRepository
import com.example.chessrepertoiretrainer.feature.stats.data.DefaultGameStatsRepository
import com.example.chessrepertoiretrainer.feature.stats.data.data.AccountSyncCoordinator
import com.example.chessrepertoiretrainer.feature.stats.data.data.StatsRefreshCoordinator
import com.example.chessrepertoiretrainer.feature.stats.domain.PlayerProfileRepository

/**
 * Simple application-level dependency container.
 *
 * Keeps heavyweight singletons (database, repositories, fetcher registry)
 * out of composables so they are not recreated during recomposition.
 */
class AppContainer(context: Context) {

    val userSettingsRepository = UserSettingsRepository(context.applicationContext)

    private val db: ChessDatabase = ChessDatabase.Companion.getDatabase(context)

    val repertoireDao = db.repertoireDao()
    val puzzleDao = db.puzzleDao()
    val playerProfileDao = db.playerProfileDao()
    val gameDao = db.gameDao()
    val gameStatsDao = db.gameStatsDao()

    val puzzleRepository = DefaultPuzzleRepository(puzzleDao)
    val playerProfileRepository = PlayerProfileRepository(playerProfileDao)

    val gameFetcherRegistry = GameFetcherRegistry(
        listOf(
            LichessGameFetcher, ChessComGameFetcher
        )
    )

    val openingTreePreparationCoordinator = OpeningTreePreparationCoordinator()
    val onlineGamesFetchCoordinator = OnlineGamesFetchCoordinator(gameFetcherRegistry)

    val gamesRepository: GamesRepository = PlayerGamesRepository(
        gameDao = gameDao, playerProfileDao = playerProfileDao, fetcherRegistry = gameFetcherRegistry
    )

    val gameStatsRepository = DefaultGameStatsRepository(
        gamesRepository = gamesRepository, gameStatsDao = gameStatsDao
    )

    val accountSyncCoordinator = AccountSyncCoordinator(
        profileRepository = playerProfileRepository, gamesRepository = gamesRepository
    )

    val statsRefreshCoordinator = StatsRefreshCoordinator(
        gameStatsRepository = gameStatsRepository
    )
}