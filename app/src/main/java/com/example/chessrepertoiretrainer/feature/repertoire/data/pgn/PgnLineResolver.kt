package com.example.chessrepertoiretrainer.feature.repertoire.data.pgn

import android.util.Log
import com.example.chessrepertoiretrainer.core.chess.domain.moveFromSan
import com.example.chessrepertoiretrainer.core.chess.domain.toSan
import com.github.bhlangonijr.chesslib.Board

class PgnLineResolver(
    private val minMovesPerLine: Int = MIN_MOVES_PER_LINE
) {

    data class ResolvedMove(
        val san: String, val fen: String, val comment: String?
    )

    fun resolveLine(
        parsedMoves: List<PgnMovetextParser.ParsedMove>, fenTag: String?
    ): List<ResolvedMove> {
        val board = Board()
        if (!fenTag.isNullOrBlank()) {
            runCatching { board.loadFromFen(fenTag) }.onFailure { error ->
                    Log.e("PgnImporter", "Nie udalo sie zaladowac FEN z tagu FEN: $fenTag", error)
                }
        }

        val resolvedMoves = mutableListOf<ResolvedMove>()

        for (parsed in parsedMoves) {
            val matchingMove = board.moveFromSan(parsed.san)
            if (matchingMove == null) {
                Log.e(
                    "PgnImporter",
                    "Nie znaleziono dopasowania SAN dla ruchu '${parsed.san}' w pozycji ${board.fen}"
                )
                break
            }

            val sanFromBoard = board.toSan(matchingMove)
            board.doMove(matchingMove)
            resolvedMoves.add(
                ResolvedMove(
                    san = sanFromBoard, fen = board.fen, comment = parsed.comment
                )
            )
        }

        if (resolvedMoves.size < minMovesPerLine) {
            return emptyList()
        }

        return resolvedMoves
    }

    private companion object {
        private const val MIN_MOVES_PER_LINE = 4
    }
}

