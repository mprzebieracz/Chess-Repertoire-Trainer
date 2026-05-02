package com.example.chessrepertoiretrainer.feature.repertoire.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.chessrepertoiretrainer.core.database.entity.Chapter
import com.example.chessrepertoiretrainer.feature.repertoire.domain.RepertoireRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChaptersViewModel(
    private val repertoireRepository: RepertoireRepository, savedStateHandle: SavedStateHandle
) : ViewModel() {

    val repertoireId: Int = checkNotNull(savedStateHandle["repertoireId"])

    val chapters: StateFlow<List<Chapter>> =
        repertoireRepository.getChaptersForRepertoire(repertoireId).stateIn(
            scope = viewModelScope,
            started = SharingStarted.Companion.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addChapter(name: String) {
        viewModelScope.launch {
            val currentChapters = chapters.value
            val sortOrder = (currentChapters.maxByOrNull { it.sortOrder }?.sortOrder ?: -1) + 1
            repertoireRepository.insertChapter(
                Chapter(
                    repertoireId = repertoireId, name = name, sortOrder = sortOrder
                )
            )
        }
    }

    fun deleteChapter(chapter: Chapter) {
        viewModelScope.launch {
            repertoireRepository.deleteChapter(chapter)
        }
    }

    class Factory(private val repertoireRepository: RepertoireRepository) :
        ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val savedStateHandle = extras.createSavedStateHandle()
            return ChaptersViewModel(repertoireRepository, savedStateHandle) as T
        }
    }
}