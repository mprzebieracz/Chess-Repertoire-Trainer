package com.example.chessrepertoiretrainer.core.chess.pgn.uci

import com.example.chessrepertoiretrainer.core.chess.domain.toSan
import com.example.chessrepertoiretrainer.core.chess.domain.uciToMove
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Piece

/**
 * Converts a sequence of UCI move strings into SAN notation, starting from [fen].
 *
 * Returns an empty list if any move is not legal in the current position (bad FEN or bad moves),
 * rather than crashing. Uses legalMoves() to look up each move so that the Move object has all
 * internal fields set before being passed to Board.doMove().
 */
fun convertUciSequenceToSan(fen: String, tokens: List<String>): List<String> {
    if (tokens.isEmpty()) return emptyList()
    return try {
        val workingBoard = Board().apply { loadFromFen(fen) }
        val sanMoves = mutableListOf<String>()

        for (uci in tokens) {
            val candidate = uciToMove(uci, workingBoard) ?: return emptyList()

            // Find the matching legal move so that the Move object has all fields set
            // (chesslib's Board.doMove / isMoveLegal needs movingPiece to be non-null).
            val legalMove = workingBoard.legalMoves().find { lm ->
                lm.from == candidate.from && lm.to == candidate.to &&
                    (candidate.promotion == Piece.NONE || lm.promotion == candidate.promotion)
            } ?: return emptyList()

            val san = workingBoard.toSan(legalMove)
            workingBoard.doMove(legalMove)
            sanMoves.add(san)
        }
        sanMoves
    } catch (e: Exception) {
        emptyList()
    }
}
