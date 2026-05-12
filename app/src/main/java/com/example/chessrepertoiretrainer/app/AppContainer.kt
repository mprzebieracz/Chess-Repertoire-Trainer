package com.example.chessrepertoiretrainer.app

import android.content.Context
import com.example.chessrepertoiretrainer.core.database.ChessDatabase
import com.example.chessrepertoiretrainer.core.engine.StockfishEngine
import com.example.chessrepertoiretrainer.core.navigation.NavTransientStore
import com.example.chessrepertoiretrainer.core.network.games.ChessComGameFetcher
import com.example.chessrepertoiretrainer.core.network.games.GameFetcherRegistry
import com.example.chessrepertoiretrainer.core.network.games.LichessGameFetcher
import com.example.chessrepertoiretrainer.feature.mygames.data.DefaultSavedGameRepository
import com.example.chessrepertoiretrainer.feature.mygames.data.GameSyncManager
import com.example.chessrepertoiretrainer.feature.puzzles.data.DefaultPuzzleRepository
import com.example.chessrepertoiretrainer.feature.repertoire.data.DefaultRepertoireRepository
import com.example.chessrepertoiretrainer.feature.repertoire.domain.RepertoireRepository
import com.example.chessrepertoiretrainer.feature.repertoire.domain.usecase.RepertoireComplianceAnalyzer
import com.example.chessrepertoiretrainer.feature.settings.data.UserSettingsRepository

class AppContainer(context: Context) {

    val userSettingsRepository = UserSettingsRepository(context.applicationContext)

    private val db: ChessDatabase = ChessDatabase.getDatabase(context)

    private val repertoireDao = db.repertoireDao()
    private val puzzleDao = db.puzzleDao()
    private val savedGameDao = db.savedGameDao()

    val puzzleRepository = DefaultPuzzleRepository(puzzleDao)
    val repertoireRepository: RepertoireRepository = DefaultRepertoireRepository(repertoireDao)
    val savedGameRepository = DefaultSavedGameRepository(savedGameDao)

    val gameFetcherRegistry = GameFetcherRegistry(listOf(LichessGameFetcher, ChessComGameFetcher))
    val gameSyncManager =
        GameSyncManager(gameFetcherRegistry, savedGameRepository, userSettingsRepository)
    private val repertoirePositionIndexDao = db.repertoirePositionIndexDao()
    val repertoireComplianceAnalyzer =
        RepertoireComplianceAnalyzer(repertoireDao, repertoirePositionIndexDao)

    val stockfishEngine = StockfishEngine(context.applicationContext, userSettingsRepository)

    val navTransientStore = NavTransientStore()
}