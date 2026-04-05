package com.example.chessrepertoiretrainer.ui.viewmodels

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.chessrepertoiretrainer.data.PgnImporter
import com.example.chessrepertoiretrainer.database.dao.RepertoireDao
import com.example.chessrepertoiretrainer.database.entities.Line
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LinesViewModel(
    private val repertoireDao: RepertoireDao,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val chapterId: Int = checkNotNull(savedStateHandle["chapterId"])

    val lines: StateFlow<List<Line>> = repertoireDao.getLinesForChapter(chapterId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addLine(name: String) {
        viewModelScope.launch {
            val chapter = repertoireDao.getChapterById(chapterId)
            val lineCount = repertoireDao.getLineCountForChapter(chapterId)

            val finalName = if (name.isBlank()) {
                "${chapter?.name ?: "Line"} #${lineCount + 1}"
            } else {
                name
            }

            repertoireDao.insertLine(
                Line(
                    chapterId = chapterId,
                    name = finalName,
                    nextReviewDate = System.currentTimeMillis(),
                    interval = 0,
                    easeFactor = 2.5f,
                    consecutiveCorrect = 0
                )
            )
        }
    }

    fun deleteLine(line: Line) {
        viewModelScope.launch {
            repertoireDao.deleteLine(line)
        }
    }

    fun importPgn(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val pgnString =
                    inputStream?.bufferedReader().use { it?.readText() } ?: return@launch

                val importer = PgnImporter(repertoireDao)
                importer.importPgnToChapter(context, pgnString, chapterId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    class Factory(private val repertoireDao: RepertoireDao) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val savedStateHandle = extras.createSavedStateHandle()
            return LinesViewModel(repertoireDao, savedStateHandle) as T
        }
    }
}