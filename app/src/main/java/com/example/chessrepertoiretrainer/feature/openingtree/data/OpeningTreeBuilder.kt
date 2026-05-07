package com.example.chessrepertoiretrainer.feature.openingtree.data

import android.util.Log
import com.example.chessrepertoiretrainer.core.chess.domain.moveFromSan
import com.example.chessrepertoiretrainer.core.chess.utils.PGNExtractor
import com.github.bhlangonijr.chesslib.Board

enum class GameOutcome { WIN, DRAW, LOSS }

data class GameForOpeningTree(
    val pgn: String, val isUserWhite: Boolean, val resultTag: String
)

data class OpeningTreeMoveAggregate(
    val moveSan: String,
    val toFen: String,
    var games: Int = 0,
    var wins: Int = 0,
    var draws: Int = 0,
    var losses: Int = 0
)

data class OpeningTreeNode(
    val fen: String,
    val parentFen: String?,
    val moveSanFromParent: String?,
    val children: MutableMap<String, OpeningTreeMoveAggregate> = mutableMapOf()
)

data class OpeningTree(
    val rootFen: String,
    val playerIsBlack: Boolean,
    internal val nodesByFen: Map<String, OpeningTreeNode>
) {
    fun getNode(fen: String): OpeningTreeNode? = nodesByFen[fen]
}

object OpeningTreeBuilder {
    fun buildTree(games: List<GameForOpeningTree>, playerIsBlack: Boolean): OpeningTree? {
        if (games.isEmpty()) return null

        val board = Board()
        val rootFen = board.fen
        val nodes = mutableMapOf<String, OpeningTreeNode>()
        nodes[rootFen] = OpeningTreeNode(
            fen = rootFen, parentFen = null, moveSanFromParent = null
        )

        games.forEachIndexed { index, game ->
            val outcome =
                outcomeFromResult(game.resultTag, game.isUserWhite) ?: return@forEachIndexed
            val sanMoves = PGNExtractor.extractSanMovesFromPgn(game.pgn)
            if (sanMoves.isEmpty()) return@forEachIndexed

            try {
                board.loadFromFen(rootFen)
                applyGameMoves(
                    board = board, nodes = nodes, sanMoves = sanMoves, outcome = outcome
                )
            }
            catch (e: Exception) {
                Log.e("OpeningTreeBuilder", "Error processing game index=$index: ${e.message}", e)
            }
        }

        return OpeningTree(rootFen = rootFen, playerIsBlack = playerIsBlack, nodesByFen = nodes)
    }

    private fun applyGameMoves(
        board: Board,
        nodes: MutableMap<String, OpeningTreeNode>,
        sanMoves: List<String>,
        outcome: GameOutcome
    ) {
        for (san in sanMoves) {
            val fenBefore = board.fen
            val node = nodes.getOrPut(fenBefore) {
                OpeningTreeNode(
                    fen = fenBefore, parentFen = null, moveSanFromParent = null
                )
            }

            val move = board.moveFromSan(san) ?: break
            board.doMove(move)
            val fenAfter = board.fen

            val aggregate = node.children.getOrPut(san) {
                OpeningTreeMoveAggregate(
                    moveSan = san, toFen = fenAfter
                )
            }

            aggregate.games++
            when (outcome) {
                GameOutcome.WIN -> aggregate.wins++
                GameOutcome.DRAW -> aggregate.draws++
                GameOutcome.LOSS -> aggregate.losses++
            }

            if (!nodes.containsKey(fenAfter)) {
                nodes[fenAfter] = OpeningTreeNode(
                    fen = fenAfter, parentFen = fenBefore, moveSanFromParent = san
                )
            }
        }
    }

    private fun outcomeFromResult(resultTag: String, isUserWhite: Boolean): GameOutcome? {
        return when (resultTag) {
            "1-0" -> if (isUserWhite) GameOutcome.WIN else GameOutcome.LOSS
            "0-1" -> if (isUserWhite) GameOutcome.LOSS else GameOutcome.WIN
            "1/2-1/2" -> GameOutcome.DRAW
            else -> null
        }
    }
}
