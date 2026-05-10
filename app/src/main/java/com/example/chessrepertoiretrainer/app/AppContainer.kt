package com.example.chessrepertoiretrainer.app

import android.content.Context
import com.example.chessrepertoiretrainer.core.database.ChessDatabase
import com.example.chessrepertoiretrainer.core.database.entity.SavedGame
import com.example.chessrepertoiretrainer.core.engine.StockfishEngine
import com.example.chessrepertoiretrainer.feature.mygames.data.DefaultSavedGameRepository
import com.example.chessrepertoiretrainer.feature.mygames.data.GameSyncManager
import com.example.chessrepertoiretrainer.feature.mygames.domain.usecase.RepertoireComplianceAnalyzer
import com.example.chessrepertoiretrainer.feature.openingtree.data.OpeningTree
import com.example.chessrepertoiretrainer.feature.openingtree.data.fetcher.ChessComGameFetcher
import com.example.chessrepertoiretrainer.feature.openingtree.data.fetcher.GameFetcherRegistry
import com.example.chessrepertoiretrainer.feature.openingtree.data.fetcher.LichessGameFetcher
import com.example.chessrepertoiretrainer.feature.puzzles.data.DefaultPuzzleRepository
import com.example.chessrepertoiretrainer.feature.repertoire.data.DefaultRepertoireRepository
import com.example.chessrepertoiretrainer.feature.repertoire.domain.RepertoireRepository
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

    // These vars pass complex non-serializable objects between navigation destinations.
    // @Volatile ensures visibility across threads (nav callbacks may run on different threads).
    @Volatile
    var latestOpeningTree: OpeningTree? = null

    @Volatile
    var latestGame: SavedGame? = null

    @Volatile
    var selectedChapterIds: List<Int>? = null

    @Volatile
    var analysisStartFen: String? = null
}