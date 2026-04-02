package com.example.chessrepertoiretrainer.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.chessrepertoiretrainer.data.Repertoire
import com.example.chessrepertoiretrainer.data.RepertoireDao
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RepertoireViewModel(private val repertoireDao: RepertoireDao) : ViewModel() {

    val repertoires: StateFlow<List<Repertoire>> = repertoireDao.getAllRepertoires()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
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
            if (modelClass.isAssignableFrom(RepertoireViewModel::class.java)) {
                return RepertoireViewModel(repertoireDao) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
