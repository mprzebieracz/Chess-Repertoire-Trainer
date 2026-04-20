package com.example.chessrepertoiretrainer.feature.repertoire.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.chessrepertoiretrainer.core.database.dao.RepertoireDao
import com.example.chessrepertoiretrainer.core.database.entity.Chapter
import com.example.chessrepertoiretrainer.core.database.entity.Repertoire
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class TrainingSelectionViewModel(private val repertoireDao: RepertoireDao) : ViewModel() {

    val repertoires: StateFlow<List<Repertoire>> = repertoireDao.getAllRepertoires().stateIn(
        scope = viewModelScope, started = SharingStarted.Companion.WhileSubscribed(5000), initialValue = emptyList()
    )

    private val _selectedRepertoireId = MutableStateFlow<Int?>(null)
    val selectedRepertoireId: StateFlow<Int?> = _selectedRepertoireId.asStateFlow()

    val chapters: StateFlow<List<Chapter>> = _selectedRepertoireId.flatMapLatest { id ->
        if (id == null) {
            flowOf(emptyList())
        }
        else {
            repertoireDao.getChaptersForRepertoire(id)
        }
    }.stateIn(
        scope = viewModelScope, started = SharingStarted.Companion.WhileSubscribed(5000), initialValue = emptyList()
    )

    fun selectRepertoire(id: Int) {
        _selectedRepertoireId.value = id
    }

    class Factory(private val repertoireDao: RepertoireDao) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TrainingSelectionViewModel(repertoireDao) as T
        }
    }
}