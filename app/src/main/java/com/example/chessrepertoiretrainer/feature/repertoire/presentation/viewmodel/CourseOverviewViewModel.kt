package com.example.chessrepertoiretrainer.feature.repertoire.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.chessrepertoiretrainer.core.database.entity.Chapter
import com.example.chessrepertoiretrainer.core.database.entity.Repertoire
import com.example.chessrepertoiretrainer.feature.repertoire.domain.RepertoireRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class CourseOverviewViewModel(
    private val repertoireRepository: RepertoireRepository, savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val repertoireId: Int = checkNotNull(savedStateHandle["repertoireId"])

    private val _repertoire = MutableStateFlow<Repertoire?>(null)
    val repertoire: StateFlow<Repertoire?> = _repertoire.asStateFlow()

    data class ChapterWithStats(
        val chapter: Chapter, val totalLines: Int, val learnedLines: Int
    )

    val chaptersWithStats: StateFlow<List<ChapterWithStats>> =
        repertoireRepository.getChaptersForRepertoire(repertoireId).mapLatest { chapters ->
            chapters.map { chapter ->
                val total = repertoireRepository.getLineCountForChapter(chapter.id)
                val learned = repertoireRepository.getLearnedLineCountForChapter(chapter.id)
                ChapterWithStats(
                    chapter = chapter, totalLines = total, learnedLines = learned
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Companion.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            _repertoire.value = repertoireRepository.getRepertoireById(repertoireId)
        }
    }

    class Factory(private val repertoireRepository: RepertoireRepository) :
        ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val handle = extras.createSavedStateHandle()
            return CourseOverviewViewModel(repertoireRepository, handle) as T
        }
    }
}