package com.example.chessrepertoiretrainer.feature.repertoire.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.chessrepertoiretrainer.core.database.dao.RepertoireDao
import com.example.chessrepertoiretrainer.core.database.entity.Repertoire
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CourseProgress(
    val repertoire: Repertoire, val totalLines: Int, val learnedLines: Int
) {
    val learnedFraction: Float
        get() = if (totalLines == 0) 0f else learnedLines.toFloat() / totalLines
}

class RepertoiresViewModel(private val repertoireDao: RepertoireDao) : ViewModel() {

    val courses: StateFlow<List<CourseProgress>> = repertoireDao.getAllRepertoires().mapLatest { repertoires ->
        repertoires.map { rep ->
            val total = repertoireDao.getLineCountForRepertoire(rep.id)
            val learned = repertoireDao.getLearnedLineCountForRepertoire(rep.id)
            CourseProgress(
                repertoire = rep, totalLines = total, learnedLines = learned
            )
        }
    }.stateIn(
        scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = emptyList()
    )

    fun addRepertoire(name: String, color: String) {
        viewModelScope.launch {
            repertoireDao.insertRepertoire(Repertoire(name = name, color = color))
        }
    }

    fun deleteRepertoire(repertoire: Repertoire) {
        viewModelScope.launch {
            repertoireDao.deleteRepertoire(repertoire)
        }
    }

    class Factory(private val repertoireDao: RepertoireDao) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return RepertoiresViewModel(repertoireDao) as T
        }
    }
}
