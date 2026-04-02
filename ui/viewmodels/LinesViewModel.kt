package com.example.chessrepertoiretrainer.ui.viewmodels

import android.os.Bundle
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.savedstate.SavedStateRegistryOwner
import com.example.chessrepertoiretrainer.data.Line
import com.example.chessrepertoiretrainer.data.RepertoireDao
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class LinesViewModel(
    private val repertoireDao: RepertoireDao,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    val chapterId: Int = savedStateHandle.get<String>("chapterId")?.toInt() ?: 0

    val lines: StateFlow<List<Line>> = repertoireDao.getLinesForChapter(chapterId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addLine(name: String) {
        viewModelScope.launch {
            val chapter = repertoireDao.getChapterById(chapterId)
            val currentLines = lines.value
            val finalName = if (name.isBlank()) {
                "${chapter?.name ?: "Line"} #${currentLines.size + 1}"
            } else name
            
            repertoireDao.insertLine(Line(chapterId = chapterId, name = finalName))
        }
    }

    fun deleteLine(line: Line) {
        viewModelScope.launch {
            repertoireDao.deleteLine(line)
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
        ): T = LinesViewModel(repertoireDao, handle) as T
    }
}
