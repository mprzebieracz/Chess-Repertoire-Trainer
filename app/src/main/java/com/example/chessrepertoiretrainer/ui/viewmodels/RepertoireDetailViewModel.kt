package com.example.chessrepertoiretrainer.ui.viewmodels

import android.os.Bundle
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.savedstate.SavedStateRegistryOwner
import com.example.chessrepertoiretrainer.data.Chapter
import com.example.chessrepertoiretrainer.data.RepertoireDao
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class RepertoireDetailViewModel(
    private val repertoireDao: RepertoireDao,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    val repertoireId: Int = savedStateHandle.get<String>("repertoireId")?.toInt() ?: 0

    val chapters: StateFlow<List<Chapter>> = repertoireDao.getChaptersForRepertoire(repertoireId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addChapter(name: String) {
        viewModelScope.launch {
            repertoireDao.insertChapter(Chapter(repertoireId = repertoireId, name = name))
        }
    }

    fun deleteChapter(chapter: Chapter) {
        viewModelScope.launch {
            repertoireDao.deleteChapter(chapter)
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
        ): T = RepertoireDetailViewModel(repertoireDao, handle) as T
    }
}
