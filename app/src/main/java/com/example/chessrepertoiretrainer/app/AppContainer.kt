package com.example.chessrepertoiretrainer.app

import android.content.Context
import com.example.chessrepertoiretrainer.core.database.ChessDatabase
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

    val repertoireDao = db.repertoireDao()
    val puzzleDao = db.puzzleDao()

    val puzzleRepository = DefaultPuzzleRepository(puzzleDao)
    val repertoireRepository: RepertoireRepository = DefaultRepertoireRepository(repertoireDao)

    val gameFetcherRegistry = GameFetcherRegistry(listOf(LichessGameFetcher, ChessComGameFetcher))

    var latestOpeningTree: OpeningTree? = null
}
