package com.example.chessrepertoiretrainer.feature.repertoire.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.chessrepertoiretrainer.core.database.entity.Chapter
import com.example.chessrepertoiretrainer.core.database.entity.Repertoire
import com.example.chessrepertoiretrainer.feature.mygames.data.SavedGameRepository
import com.example.chessrepertoiretrainer.feature.mygames.domain.model.ChapterStats
import com.example.chessrepertoiretrainer.feature.mygames.domain.model.toChapterStats
import com.example.chessrepertoiretrainer.feature.repertoire.domain.RepertoireRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class CourseOverviewViewModel(
    private val repertoireRepository: RepertoireRepository,
    private val savedGameRepository: SavedGameRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val repertoireId: Int = checkNotNull(savedStateHandle["repertoireId"])

    private val _repertoire = MutableStateFlow<Repertoire?>(null)
    val repertoire: StateFlow<Repertoire?> = _repertoire.asStateFlow()

    data class ChapterWithStats(
        val chapter: Chapter,
        val totalLines: Int,
        val learnedLines: Int,
        val gameStats: ChapterStats? = null
    )

    val chaptersWithStats: StateFlow<List<ChapterWithStats>> =
        combine(
            repertoireRepository.getChaptersForRepertoire(repertoireId),
            savedGameRepository.observeMatchCount()
        ) { chapters, _ -> chapters }
        .mapLatest { chapters ->
            chapters.map { chapter ->
                val total = repertoireRepository.getLineCountForChapter(chapter.id)
                val learned = repertoireRepository.getLearnedLineCountForChapter(chapter.id)
                val gameStats = savedGameRepository
                    .getStatsByChapter(chapter.id, isWhite = null, since = 0L)
                    ?.takeIf { it.played > 0 }
                    ?.toChapterStats()
                ChapterWithStats(
                    chapter = chapter,
                    totalLines = total,
                    learnedLines = learned,
                    gameStats = gameStats
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _showChapterSelection = MutableStateFlow(false)
    val showChapterSelection: StateFlow<Boolean> = _showChapterSelection.asStateFlow()

    private val _selectedChapterIds = MutableStateFlow<Set<Int>>(emptySet())
    val selectedChapterIds: StateFlow<Set<Int>> = _selectedChapterIds.asStateFlow()

    init {
        viewModelScope.launch {
            _repertoire.value = repertoireRepository.getRepertoireById(repertoireId)
        }
    }

    fun openChapterSelection() {
        _selectedChapterIds.value = emptySet()
        _showChapterSelection.value = true
    }

    fun dismissChapterSelection() {
        _showChapterSelection.value = false
    }

    fun toggleChapterSelection(chapterId: Int) {
        _selectedChapterIds.update { if (chapterId in it) it - chapterId else it + chapterId }
    }

    class Factory(
        private val repertoireRepository: RepertoireRepository,
        private val savedGameRepository: SavedGameRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val handle = extras.createSavedStateHandle()
            return CourseOverviewViewModel(repertoireRepository, savedGameRepository, handle) as T
        }
    }
}