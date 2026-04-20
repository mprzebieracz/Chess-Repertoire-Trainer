package com.example.chessrepertoiretrainer.data

import android.util.Log
import com.example.chessrepertoiretrainer.ui.components.chess.moveFromSan
import com.github.bhlangonijr.chesslib.Board

/** Outcome of a single game from the tracked player's perspective. */
enum class GameOutcome { WIN, DRAW, LOSS }

/** Minimal model used for building the opening tree from PGNs. */
data class GameForOpeningTree(
    val pgn: String, val isUserWhite: Boolean, val resultTag: String
)

/** Aggregated statistics for a specific move from a particular position. */
data class OpeningTreeMoveAggregate(
    val moveSan: String, val toFen: String, var games: Int = 0, var wins: Int = 0, var draws: Int = 0, var losses: Int = 0
)

/** Single node in the opening tree, keyed by FEN. */
data class OpeningTreeNode(
    val fen: String,
    val parentFen: String?,
    val moveSanFromParent: String?,
    val children: MutableMap<String, OpeningTreeMoveAggregate> = mutableMapOf()
)

/** In-memory opening tree representation built from the user's games. */
data class OpeningTree(
    val rootFen: String, internal val nodesByFen: Map<String, OpeningTreeNode>
) {
    fun getNode(fen: String): OpeningTreeNode? = nodesByFen[fen]
}

/**
 * Static helper for building an [OpeningTree] from a list of games.
 */
object OpeningTreeBuilder {

    /** Build an opening tree from the given games, or null if there are no usable games. */
    fun buildTree(games: List<GameForOpeningTree>): OpeningTree? {
        if (games.isEmpty()) return null

        val board = Board()
        val rootFen = board.fen
        val nodes = mutableMapOf<String, OpeningTreeNode>()
        nodes[rootFen] = OpeningTreeNode(
            fen = rootFen, parentFen = null, moveSanFromParent = null
        )

        games.forEachIndexed { index, game ->
            val outcome = outcomeFromResult(game.resultTag, game.isUserWhite) ?: return@forEachIndexed

            val sanMoves = extractSanMovesFromPgn(game.pgn)
            if (sanMoves.isEmpty()) return@forEachIndexed

            try {
                board.loadFromFen(rootFen)
            }
            catch (e: Exception) {
                Log.e("OpeningTreeBuilder", "Failed to load root FEN: ${e.message}", e)
                return@forEachIndexed
            }

            try {
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

                    val childAgg = node.children.getOrPut(san) {
                        OpeningTreeMoveAggregate(
                            moveSan = san, toFen = fenAfter
                        )
                    }

                    childAgg.games++
                    when (outcome) {
                        GameOutcome.WIN -> childAgg.wins++
                        GameOutcome.DRAW -> childAgg.draws++
                        GameOutcome.LOSS -> childAgg.losses++
                    }

                    // Ensure child node exists with parent pointer on first encounter.
                    if (!nodes.containsKey(fenAfter)) {
                        nodes[fenAfter] = OpeningTreeNode(
                            fen = fenAfter, parentFen = fenBefore, moveSanFromParent = san
                        )
                    }
                }
            }
            catch (e: Exception) {
                Log.e("OpeningTreeBuilder", "Error processing game index=$index: ${e.message}", e)
            }
        }

        return OpeningTree(rootFen = rootFen, nodesByFen = nodes)
    }

    private fun outcomeFromResult(resultTag: String, isUserWhite: Boolean): GameOutcome? {
        return when (resultTag) {
            "1-0" -> if (isUserWhite) GameOutcome.WIN else GameOutcome.LOSS
            "0-1" -> if (isUserWhite) GameOutcome.LOSS else GameOutcome.WIN
            "1/2-1/2" -> GameOutcome.DRAW
            else -> null
        }
    }

    /**
     * Very small PGN movetext extractor that returns a flat list of SAN moves
     * from the main line. Comments, NAGs, headers and game results are
     * stripped. Variations in parentheses are flattened and mostly ignored.
     */
    internal fun extractSanMovesFromPgn(pgn: String): List<String> {
        if (pgn.isBlank()) return emptyList()

        // 1. Drop header lines ([Event ...]) and leading blank lines. Keep only the movetext.
        val bodyLines = mutableListOf<String>()
        var inBody = false

        pgn.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            if (!inBody) {
                if (line.startsWith("[")) {
                    // Header line – skip
                    return@forEach
                }
                if (line.isBlank()) {
                    // Blank line between headers and body – skip
                    return@forEach
                }
                // First non-header, non-blank line – movetext begins here.
                inBody = true
            }

            if (inBody) {
                bodyLines += rawLine
            }
        }

        if (bodyLines.isEmpty()) return emptyList()

        // 2. Tokenize manually while skipping comments, NAGs, engine tags, and move numbers.
        val sanMoves = mutableListOf<String>()
        var inBraceComment = false

        fun isGameResultToken(token: String): Boolean = token == "1-0" || token == "0-1" || token == "1/2-1/2" || token == "*"

        fun isMoveNumberToken(token: String): Boolean {
            // Matches things like "1.", "12.", "34..." etc. without using regex.
            if (token.isEmpty()) return false
            var i = 0
            val n = token.length
            while (i < n && token[i].isDigit()) i++
            if (i == 0 || i >= n) return false
            if (token[i] != '.') return false
            // allow one or more dots, e.g. "1.", "1..."
            while (i < n && token[i] == '.') i++
            return i == n
        }

        for (rawLine in bodyLines) {
            var line = rawLine
            // Very simple removal of engine tags [%...]; safe even with nested brackets.
            while (true) {
                val start = line.indexOf("[%")
                if (start == -1) break
                val end = line.indexOf(']', start + 2)
                if (end == -1) {
                    line = line.removeRange(start, line.length)
                    break
                }
                else {
                    line = line.removeRange(start, end + 1)
                }
            }

            val pieces = line.split(' ', '\t')
            for (piece in pieces) {
                var token = piece.trim()
                if (token.isEmpty()) continue

                // Handle comments in { ... }
                if (inBraceComment) {
                    if (token.contains('}')) {
                        inBraceComment = false
                    }
                    continue
                }
                if (token.startsWith("{")) {
                    if (!token.contains('}')) {
                        inBraceComment = true
                    }
                    continue
                }

                // Numeric annotation glyphs like $1, $15 – skip.
                if (token.startsWith('$') && token.drop(1).all { it.isDigit() }) continue

                // Game result token – stop.
                if (isGameResultToken(token)) break

                // Strip simple surrounding parentheses used for variations.
                token = token.trim('(', ')')
                if (token.isEmpty()) continue

                // Move numbers like 1. or 12... – skip.
                if (isMoveNumberToken(token)) continue

                sanMoves += token
            }
        }

        return sanMoves
    }
}

