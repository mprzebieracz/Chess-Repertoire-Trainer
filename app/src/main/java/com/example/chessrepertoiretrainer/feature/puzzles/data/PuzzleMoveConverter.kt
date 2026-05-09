package com.example.chessrepertoiretrainer.feature.puzzles.data

import com.example.chessrepertoiretrainer.core.chess.domain.toSan
import com.example.chessrepertoiretrainer.core.chess.domain.uciToMove
import com.github.bhlangonijr.chesslib.Board

fun convertUciSequenceToSan(fen: String, tokens: List<String>): List<String> {
    if (tokens.isEmpty()) return emptyList()

    val workingBoard = Board().apply { loadFromFen(fen) }
    val sanMoves = mutableListOf<String>()

    for (uci in tokens) {
        val move = uciToMove(uci, workingBoard) ?: return emptyList()

        val san = workingBoard.toSan(move)
        workingBoard.doMove(move)
        sanMoves.add(san)
    }

    return sanMoves
}