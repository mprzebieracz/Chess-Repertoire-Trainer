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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class EditCourseViewModel(
    private val repertoireRepository: RepertoireRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val repertoireId: Int = checkNotNull(savedStateHandle["repertoireId"])

    private val _repertoire = MutableStateFlow<Repertoire?>(null)
    val repertoire: StateFlow<Repertoire?> = _repertoire.asStateFlow()

    val chapters: StateFlow<List<Chapter>> =
        repertoireRepository.getChaptersForRepertoire(repertoireId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _renamingChapterId = MutableStateFlow<Int?>(null)
    val renamingChapterId: StateFlow<Int?> = _renamingChapterId.asStateFlow()

    init {
        viewModelScope.launch {
            _repertoire.value = repertoireRepository.getRepertoireById(repertoireId)
        }
    }

    fun renameRepertoire(newName: String) {
        val current = _repertoire.value ?: return
        viewModelScope.launch {
            val updated = current.copy(name = newName)
            repertoireRepository.updateRepertoire(updated)
            _repertoire.value = updated
        }
    }

    fun deleteRepertoire(onDeleted: () -> Unit) {
        val current = _repertoire.value ?: return
        viewModelScope.launch {
            repertoireRepository.deleteRepertoire(current)
            onDeleted()
        }
    }

    fun addChapter(name: String) {
        viewModelScope.launch {
            val sortOrder = (chapters.value.maxByOrNull { it.sortOrder }?.sortOrder ?: -1) + 1
            repertoireRepository.insertChapter(
                Chapter(
                    repertoireId = repertoireId,
                    name = name,
                    sortOrder = sortOrder
                )
            )
        }
    }

    fun deleteChapter(chapter: Chapter) {
        viewModelScope.launch { repertoireRepository.deleteChapter(chapter) }
    }

    fun startRename(chapterId: Int) {
        _renamingChapterId.value = chapterId
    }

    fun cancelRename() {
        _renamingChapterId.value = null
    }

    fun renameChapter(chapter: Chapter, newName: String) {
        viewModelScope.launch {
            repertoireRepository.updateChapter(chapter.copy(name = newName))
            _renamingChapterId.value = null
        }
    }

    fun persistChapterOrder(orderedChapters: List<Chapter>) {
        viewModelScope.launch {
            orderedChapters.forEachIndexed { index, chapter ->
                if (chapter.sortOrder != index) {
                    repertoireRepository.updateChapter(chapter.copy(sortOrder = index))
                }
            }
        }
    }

    class Factory(private val repo: RepertoireRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
            EditCourseViewModel(repo, extras.createSavedStateHandle()) as T
    }
}