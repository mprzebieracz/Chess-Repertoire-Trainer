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
import com.example.chessrepertoiretrainer.core.activity.ActivityRecorder
import com.example.chessrepertoiretrainer.core.network.explorer.LichessExplorerService
import com.example.chessrepertoiretrainer.core.opening.OpeningRegistry
import com.example.chessrepertoiretrainer.core.bluetooth.BluetoothTransfer
import com.example.chessrepertoiretrainer.core.repertoire.GameChapterMatcher
import com.example.chessrepertoiretrainer.feature.repertoire.data.transfer.RepertoireExporter
import com.example.chessrepertoiretrainer.feature.repertoire.data.transfer.RepertoireImporter
import com.example.chessrepertoiretrainer.feature.mygames.domain.OnDemandGameAnalyzer
import com.example.chessrepertoiretrainer.feature.settings.data.UserSettingsRepository

class AppContainer(context: Context) {

    val userSettingsRepository = UserSettingsRepository(context.applicationContext)
    val openingRegistry = OpeningRegistry(context.applicationContext.assets)

    private val db: ChessDatabase = ChessDatabase.getDatabase(context)

    val repertoireDao = db.repertoireDao()
    val puzzleDao = db.puzzleDao()
    private val savedGameDao = db.savedGameDao()
    private val repertoirePositionIndexDao = db.repertoirePositionIndexDao()
    val dailyActivityDao = db.dailyActivityDao()
    val reviewLogDao = db.reviewLogDao()
    val savedGameRepertoireMatchDao = db.savedGameRepertoireMatchDao()
    val moveEvalDao = db.moveEvalDao()
    val lichessExplorerCacheDao = db.lichessExplorerCacheDao()

    val repertoireOpeningDao = db.repertoireOpeningDao()
    val puzzleRepository = DefaultPuzzleRepository(puzzleDao, repertoireOpeningDao)
    val repertoireRepository: RepertoireRepository = DefaultRepertoireRepository(repertoireDao)
    val savedGameRepository = DefaultSavedGameRepository(savedGameDao)

    val gameFetcherRegistry = GameFetcherRegistry(listOf(LichessGameFetcher, ChessComGameFetcher))
    val repertoireComplianceAnalyzer =
        RepertoireComplianceAnalyzer(repertoireDao, repertoirePositionIndexDao)

    val activityRecorder = ActivityRecorder(dailyActivityDao, reviewLogDao, repertoireDao)
    val gameChapterMatcher = GameChapterMatcher(repertoireComplianceAnalyzer, savedGameRepertoireMatchDao)
    val lichessExplorerService = LichessExplorerService(lichessExplorerCacheDao, userSettingsRepository)
    val gameSyncManager = GameSyncManager(
        gameFetcherRegistry, savedGameRepository, userSettingsRepository,
        gameChapterMatcher, activityRecorder
    )

    val stockfishEngine = StockfishEngine(context.applicationContext, userSettingsRepository)

    val onDemandGameAnalyzer = OnDemandGameAnalyzer(stockfishEngine, moveEvalDao)

    val navTransientStore = NavTransientStore()

    val bluetoothTransfer = BluetoothTransfer(context.applicationContext)
    val repertoireExporter = RepertoireExporter(repertoireDao)
    val repertoireImporter = RepertoireImporter(repertoireDao)
}