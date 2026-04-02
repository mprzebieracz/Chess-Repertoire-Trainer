package com.example.chessrepertoiretrainer.data

import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.game.Game
import com.github.bhlangonijr.chesslib.move.Move
import com.github.bhlangonijr.chesslib.move.MoveList
import com.github.bhlangonijr.chesslib.pgn.PgnHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

class PgnImporter(private val repertoireDao: RepertoireDao) {

    suspend fun importPgn(inputStream: InputStream, chapterId: Int) = withContext(Dispatchers.IO) {
        val pgnContent = inputStream.bufferedReader().use { it.readText() }
        val pgn = PgnHolder("")
        pgn.loadPgn(pgnContent)
        
        for (game in pgn.games) {
            importGame(game, chapterId)
        }
    }

    private suspend fun importGame(game: Game, chapterId: Int) {
        val board = Board()
        val moveList = game.halfMoves
        
        // Create a new line for each imported game
        val lineId = repertoireDao.insertLine(
            Line(chapterId = chapterId, name = game.whitePlayer.name + " vs " + game.blackPlayer.name)
        ).toInt()
        
        importMoveList(moveList, board, lineId)
    }

    private suspend fun importMoveList(
        moveList: MoveList,
        board: Board,
        lineId: Int
    ) {
        val moves = moveList.toList()
        
        for (i in moves.indices) {
            val move = moves[i]
            val moveSan = generateSan(board, move)
            board.doMove(move)
            val positionFen = board.fen
            
            val moveEntity = LineMove(
                lineId = lineId,
                moveIndex = i,
                moveSan = moveSan,
                fen = positionFen
            )
            
            repertoireDao.insertLineMove(moveEntity)
        }
    }

    private fun generateSan(board: Board, move: Move): String {
        val piece = board.getPiece(move.from)
        val isCapture = board.getPiece(move.to) != Piece.NONE
        
        if (piece == Piece.WHITE_KING || piece == Piece.BLACK_KING) {
            if (move.from == Square.E1 && move.to == Square.G1) return "O-O"
            if (move.from == Square.E1 && move.to == Square.C1) return "O-O-O"
            if (move.from == Square.E8 && move.to == Square.G8) return "O-O"
            if (move.from == Square.E8 && move.to == Square.C8) return "O-O-O"
        }

        val piecePrefix = when (piece) {
            Piece.WHITE_KNIGHT, Piece.BLACK_KNIGHT -> "N"
            Piece.WHITE_BISHOP, Piece.BLACK_BISHOP -> "B"
            Piece.WHITE_ROOK, Piece.BLACK_ROOK -> "R"
            Piece.WHITE_QUEEN, Piece.BLACK_QUEEN -> "Q"
            Piece.WHITE_KING, Piece.BLACK_KING -> "K"
            else -> ""
        }

        val destination = move.to.toString().lowercase()
        val captureSign = if (isCapture) "x" else ""
        
        return if (piecePrefix == "") {
            if (isCapture) "${move.from.toString().lowercase()[0]}x$destination" else destination
        } else {
            "$piecePrefix$captureSign$destination"
        }
    }
}
