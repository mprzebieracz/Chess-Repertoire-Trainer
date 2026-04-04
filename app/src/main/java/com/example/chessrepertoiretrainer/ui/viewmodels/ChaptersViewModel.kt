package com.example.chessrepertoiretrainer.ui.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.chessrepertoiretrainer.database.dao.RepertoireDao
import com.example.chessrepertoiretrainer.database.entities.Chapter
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChaptersViewModel(
    private val repertoireDao: RepertoireDao,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val repertoireId: Int = checkNotNull(savedStateHandle["repertoireId"])

    val chapters: StateFlow<List<Chapter>> = repertoireDao.getChaptersForRepertoire(repertoireId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addChapter(name: String) {
        viewModelScope.launch {
            val currentChapters = chapters.value
            val sortOrder = (currentChapters.maxByOrNull { it.sortOrder }?.sortOrder ?: -1) + 1
            repertoireDao.insertChapter(
                Chapter(
                    repertoireId = repertoireId,
                    name = name,
                    sortOrder = sortOrder
                )
            )
        }
    }

    fun deleteChapter(chapter: Chapter) {
        viewModelScope.launch {
            repertoireDao.deleteChapter(chapter)
        }
    }

    class Factory(private val repertoireDao: RepertoireDao) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val savedStateHandle = extras.createSavedStateHandle()
            return ChaptersViewModel(repertoireDao, savedStateHandle) as T
        }
    }
}
