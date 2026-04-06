package com.example.chessrepertoiretrainer.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.SavedStateHandle
import com.example.chessrepertoiretrainer.database.dao.RepertoireDao
import com.example.chessrepertoiretrainer.database.entities.Chapter
import com.example.chessrepertoiretrainer.database.entities.Repertoire
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for the repertoire "course overview" screen.
 *
 * It exposes the selected repertoire together with its chapters and some
 * lightweight statistics (for now, just total line count per chapter). This
 * screen is the natural entry point for learning/training a course.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CourseOverviewViewModel(
    private val repertoireDao: RepertoireDao,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val repertoireId: Int = checkNotNull(savedStateHandle["repertoireId"])

    private val _repertoire = MutableStateFlow<Repertoire?>(null)
    val repertoire: StateFlow<Repertoire?> = _repertoire.asStateFlow()

    data class ChapterWithStats(
        val chapter: Chapter,
        val totalLines: Int,
        val learnedLines: Int
    )

    val chaptersWithStats: StateFlow<List<ChapterWithStats>> =
        repertoireDao.getChaptersForRepertoire(repertoireId)
            .mapLatest { chapters ->
                chapters.map { chapter ->
                    val total = repertoireDao.getLineCountForChapter(chapter.id)
                    val learned = repertoireDao.getLearnedLineCountForChapter(chapter.id)
                    ChapterWithStats(
                        chapter = chapter,
                        totalLines = total,
                        learnedLines = learned
                    )
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    init {
        viewModelScope.launch {
            _repertoire.value = repertoireDao.getRepertoireById(repertoireId)
        }
    }

    class Factory(private val repertoireDao: RepertoireDao) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val handle = extras.createSavedStateHandle()
            return CourseOverviewViewModel(repertoireDao, handle) as T
        }
    }
}


