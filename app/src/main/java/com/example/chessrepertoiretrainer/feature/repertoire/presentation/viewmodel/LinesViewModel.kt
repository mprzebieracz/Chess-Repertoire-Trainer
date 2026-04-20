package com.example.chessrepertoiretrainer.feature.repertoire.presentation.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.chessrepertoiretrainer.core.database.entity.Line
import com.example.chessrepertoiretrainer.feature.repertoire.domain.RepertoireRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LinesViewModel(
    private val repertoireRepository: RepertoireRepository, savedStateHandle: SavedStateHandle
) : ViewModel() {

    val chapterId: Int = checkNotNull(savedStateHandle["chapterId"])

    val lines: StateFlow<List<Line>> = repertoireRepository.getLinesForChapter(chapterId).stateIn(
        scope = viewModelScope, started = SharingStarted.Companion.WhileSubscribed(5000), initialValue = emptyList()
    )

    fun addLine(name: String) {
        viewModelScope.launch {
            val chapter = repertoireRepository.getChapterById(chapterId)
            val lineCount = repertoireRepository.getLineCountForChapter(chapterId)

            val finalName = if (name.isBlank()) {
                "${chapter?.name ?: "Line"} #${lineCount + 1}"
            }
            else {
                name
            }

            repertoireRepository.insertLine(
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
            repertoireRepository.deleteLine(line)
        }
    }

    fun importPgn(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val pgnString = inputStream?.bufferedReader().use { it?.readText() } ?: return@launch

                repertoireRepository.importPgnToChapter(pgnString = pgnString, chapterId = chapterId)
            }
            catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    class Factory(private val repertoireRepository: RepertoireRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val savedStateHandle = extras.createSavedStateHandle()
            return LinesViewModel(repertoireRepository, savedStateHandle) as T
        }
    }
}