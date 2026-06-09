package com.example.chessrepertoiretrainer.feature.mygames.domain

import com.example.chessrepertoiretrainer.core.chess.domain.findLegalMoveBySan
import com.example.chessrepertoiretrainer.core.chess.pgn.extract.PGNExtractor
import com.example.chessrepertoiretrainer.core.database.dao.MoveEvalDao
import com.example.chessrepertoiretrainer.core.database.entity.MoveEval
import com.example.chessrepertoiretrainer.core.database.entity.SavedGame
import com.example.chessrepertoiretrainer.core.engine.EngineAnalysis
import com.example.chessrepertoiretrainer.core.engine.StockfishEngine
import com.github.bhlangonijr.chesslib.Board
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

class OnDemandGameAnalyzer(
    private val engine: StockfishEngine,
    private val moveEvalDao: MoveEvalDao,
    private val targetDepth: Int = 15
) {

    suspend fun loadCachedEvals(gameId: String): List<MoveEval> =
        moveEvalDao.getEvalsForGame(gameId)

    suspend fun analyzeGame(
        game: SavedGame,
        onProgress: (analyzed: Int, total: Int) -> Unit = { _, _ -> }
    ): List<MoveEval> {
        if (moveEvalDao.hasEvalsForGame(game.id)) return moveEvalDao.getEvalsForGame(game.id)

        val sans = PGNExtractor.extractSanMovesFromPgn(game.pgn)
        if (sans.isEmpty()) return emptyList()

        if (engine.isEnabled.value) engine.disable()

        val results = mutableListOf<MoveEval>()
        val board = Board()
        var prevCp: Int? = null

        try {
            for ((index, san) in sans.withIndex()) {
                onProgress(index, sans.size)
                val isWhiteMove = index % 2 == 0

                val move = board.findLegalMoveBySan(san) ?: break
                board.doMove(move)
                val fen = board.fen

                val analysis = getAnalysisAt(fen)
                val cp = analysis?.centipawns

                val cpLoss = if (prevCp != null && cp != null) {
                    if (isWhiteMove) (prevCp!! - cp).coerceAtLeast(0)
                    else (cp - prevCp!!).coerceAtLeast(0)
                }
                else 0

                results.add(
                    MoveEval(
                        gameId = game.id,
                        moveIndex = index,
                        fen = fen,
                        centipawns = cp,
                        mateIn = analysis?.mateIn,
                        depth = analysis?.depth ?: 0,
                        classification = classify(cpLoss)
                    )
                )
                prevCp = cp
            }
        }
        finally {
            engine.disable()
        }

        moveEvalDao.upsertEvals(results)
        return results
    }

    private suspend fun getAnalysisAt(fen: String): EngineAnalysis? {
        engine.analyzePositionForBatch(fen, targetDepth)
        return withTimeoutOrNull(8_000L) {
            engine.analysis.filterNotNull()
                .filter { it.depth >= targetDepth || it.mateIn != null }
                .first()
        }
    }

    internal fun classify(cpLoss: Int) = when {
        cpLoss <= 10 -> "BEST"
        cpLoss <= 25 -> "GOOD"
        cpLoss <= 50 -> "INACCURACY"
        cpLoss <= 100 -> "MISTAKE"
        else -> "BLUNDER"
    }
}