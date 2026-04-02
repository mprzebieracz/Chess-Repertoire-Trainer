package com.example.chessrepertoiretrainer.ui.viewmodels

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.savedstate.SavedStateRegistryOwner
import com.example.chessrepertoiretrainer.data.PgnImporter
import com.example.chessrepertoiretrainer.data.RepertoireDao
import com.example.chessrepertoiretrainer.ui.components.chess.ChessBoardState
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.move.Move
import kotlinx.coroutines.launch

class ChapterEditorViewModel(
    private val repertoireDao: RepertoireDao,
    private val savedStateHandle: SavedStateHandle
) : ViewModel(), ChessBoardState {
    private val chapterId: Int = savedStateHandle.get<String>("chapterId")?.toInt() ?: 0
    private val board = Board()
    private val pgnImporter = PgnImporter(repertoireDao)

    override var boardState by mutableStateOf(board.fen)
        private set

    override var selectedSquare by mutableStateOf<Square?>(null)
        private set

    override var lastMove by mutableStateOf<Move?>(null)
        private set

    override var hoveredSquare by mutableStateOf<Square?>(null)

    override var isFlipped by mutableStateOf(false)
        private set

    init {
        loadRepertoireInfo()
    }

    private fun loadRepertoireInfo() {
        viewModelScope.launch {
            val chapter = repertoireDao.getChapterById(chapterId)
            val repertoire = chapter?.let { repertoireDao.getRepertoireById(it.repertoireId) }
            if (repertoire != null) {
                isFlipped = repertoire.color.equals("Black", ignoreCase = true)
            }
        }
    }

    override fun getBoard(): Board = board

    override fun onSquareClick(square: Square) {
        val currentSelected = selectedSquare
        if (currentSelected == null) {
            val piece = board.getPiece(square)
            if (piece != Piece.NONE && piece.pieceSide == board.sideToMove) {
                selectedSquare = square
            }
        } else {
            if (currentSelected == square) {
                selectedSquare = null
                return
            }
            onMove(Move(currentSelected, square))
        }
    }

    override fun onMove(move: Move) {
        if (board.legalMoves().contains(move)) {
            board.doMove(move)
            boardState = board.fen
            selectedSquare = null
        } else {
            selectedSquare = null
        }
    }

    fun importPgn(context: Context, uri: Uri) {
        viewModelScope.launch {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                pgnImporter.importPgn(inputStream, chapterId)
            }
        }
    }

    class Factory(
        private val repertoireDao: RepertoireDao,
        owner: SavedStateRegistryOwner,
        defaultArgs: Bundle? = null
    ) : AbstractSavedStateViewModelFactory(owner, defaultArgs) {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(
            key: String,
            modelClass: Class<T>,
            handle: SavedStateHandle
        ): T = ChapterEditorViewModel(repertoireDao, handle) as T
    }
}
