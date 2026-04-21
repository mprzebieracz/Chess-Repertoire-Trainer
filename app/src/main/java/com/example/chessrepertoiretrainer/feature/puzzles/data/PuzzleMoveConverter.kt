package com.example.chessrepertoiretrainer.feature.puzzles.data

import com.example.chessrepertoiretrainer.core.chess.domain.toSan
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.Side
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.move.Move

fun uciToMove(uci: String, board: Board): Move? {
    if (uci.length !in 4..5) return null

    fun coordToSquare(file: Char, rank: Char): Square? {
        if (file !in 'a'..'h' && file !in 'A'..'H') return null
        if (rank !in '1'..'8') return null

        val fileIndex = (file.lowercaseChar() - 'a')
        val rankIndex = (rank - '1') // 0 for rank '1'
        val idx = rankIndex * 8 + fileIndex

        return Square.values().getOrNull(idx)
    }

    val fromFile = uci[0]
    val fromRank = uci[1]
    val toFile = uci[2]
    val toRank = uci[3]

    val from = coordToSquare(fromFile, fromRank) ?: return null
    val to = coordToSquare(toFile, toRank) ?: return null

    val promotionChar = if (uci.length == 5) uci[4].lowercaseChar() else null
    val promotionPiece: Piece = when (promotionChar) {
        'q' -> if (board.sideToMove == Side.WHITE) Piece.WHITE_QUEEN else Piece.BLACK_QUEEN
        'r' -> if (board.sideToMove == Side.WHITE) Piece.WHITE_ROOK else Piece.BLACK_ROOK
        'b' -> if (board.sideToMove == Side.WHITE) Piece.WHITE_BISHOP else Piece.BLACK_BISHOP
        'n' -> if (board.sideToMove == Side.WHITE) Piece.WHITE_KNIGHT else Piece.BLACK_KNIGHT
        null -> Piece.NONE
        else -> return null
    }

    return if (promotionPiece == Piece.NONE) {
        Move(from, to)
    }
    else {
        Move(from, to, promotionPiece)
    }
}

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


