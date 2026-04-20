package com.example.chessrepertoiretrainer.core.chess.domain

import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.move.Move

private fun Piece.isPawn(): Boolean = this == Piece.WHITE_PAWN || this == Piece.BLACK_PAWN

private fun promotionSuffix(piece: Piece): String = when (piece) {
    Piece.WHITE_QUEEN, Piece.BLACK_QUEEN -> "=Q"
    Piece.WHITE_ROOK, Piece.BLACK_ROOK -> "=R"
    Piece.WHITE_BISHOP, Piece.BLACK_BISHOP -> "=B"
    Piece.WHITE_KNIGHT, Piece.BLACK_KNIGHT -> "=N"
    else -> ""
}

private fun Piece.prefixLetter(): String = when (this) {
    Piece.WHITE_KNIGHT, Piece.BLACK_KNIGHT -> "N"
    Piece.WHITE_BISHOP, Piece.BLACK_BISHOP -> "B"
    Piece.WHITE_ROOK, Piece.BLACK_ROOK -> "R"
    Piece.WHITE_QUEEN, Piece.BLACK_QUEEN -> "Q"
    Piece.WHITE_KING, Piece.BLACK_KING -> "K"
    else -> ""
}

fun Board.toSan(move: Move): String {
    val piece = getPiece(move.from)
    val targetPiece = getPiece(move.to)
    val isEnPassantCapture = piece.isPawn() && targetPiece == Piece.NONE && move.from.file != move.to.file
    val isCapture = targetPiece != Piece.NONE || isEnPassantCapture
    val isPromotion = move.promotion != Piece.NONE

    if (piece == Piece.WHITE_KING || piece == Piece.BLACK_KING) {
        if (move.from == Square.E1 && move.to == Square.G1) return "O-O"
        if (move.from == Square.E1 && move.to == Square.C1) return "O-O-O"
        if (move.from == Square.E8 && move.to == Square.G8) return "O-O"
        if (move.from == Square.E8 && move.to == Square.C8) return "O-O-O"
    }

    val piecePrefix = piece.prefixLetter()
    val destination = move.to.toString().lowercase()
    val captureSign = if (isCapture) "x" else ""
    val promotionSuffix = if (isPromotion) promotionSuffix(move.promotion) else ""

    val sanCore = if (piecePrefix.isEmpty()) {
        val fromFileChar = move.from.toString().lowercase()[0]
        val base = if (isCapture) "${fromFileChar}x$destination" else destination
        base + promotionSuffix
    }
    else {
        val samePieceMoves = legalMoves().filter { candidate ->
            candidate.to == move.to && getPiece(candidate.from) == piece && candidate != move
        }

        val disambiguation = if (samePieceMoves.isEmpty()) {
            ""
        }
        else {
            val fromFileChar = move.from.toString()[0].lowercaseChar()
            val fromRankChar = move.from.toString()[1]
            val otherSameFile = samePieceMoves.any { it.from.toString()[0].lowercaseChar() == fromFileChar }
            val otherSameRank = samePieceMoves.any { it.from.toString()[1] == fromRankChar }

            when {
                !otherSameFile -> fromFileChar.toString()
                !otherSameRank -> fromRankChar.toString()
                else -> move.from.toString().lowercase()
            }
        }

        buildString {
            append(piecePrefix)
            append(disambiguation)
            append(captureSign)
            append(destination)
            append(promotionSuffix)
        }
    }

    return try {
        val boardCopy = Board()
        boardCopy.loadFromFen(fen)
        boardCopy.doMove(move)

        val isMate = boardCopy.isMated
        val isCheck = !isMate && boardCopy.isKingAttacked

        buildString {
            append(sanCore)
            when {
                isMate -> append("#")
                isCheck -> append("+")
            }
        }
    }
    catch (e: Exception) {
        sanCore
    }
}

/**
 * Znajduje wśród legalnych ruchów na tej planszy taki, którego SAN odpowiada
 * podanemu ciągowi. Używa lokalnej normalizacji SAN (ignoruje +, #, !, ? na końcu).
 */
fun Board.moveFromSan(san: String): Move? {
    fun normalizeSanForMatch(value: String): String = value.trim().trimEnd('+', '#', '!', '?')

    val target = normalizeSanForMatch(san)
    return legalMoves().firstOrNull { move ->
        val sanFromBoard = toSan(move)
        normalizeSanForMatch(sanFromBoard) == target
    }
}

