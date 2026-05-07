package com.example.chessrepertoiretrainer.feature.repertoire.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.chessrepertoiretrainer.core.database.entity.Repertoire
import com.example.chessrepertoiretrainer.feature.mygames.domain.usecase.RepertoireComplianceAnalyzer
import com.example.chessrepertoiretrainer.feature.repertoire.domain.RepertoireRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CourseProgress(
    val repertoire: Repertoire, val totalLines: Int, val learnedLines: Int
) {
    val learnedFraction: Float
        get() = if (totalLines == 0) 0f else learnedLines.toFloat() / totalLines
}

@OptIn(ExperimentalCoroutinesApi::class)
class RepertoiresViewModel(
    private val repertoireRepository: RepertoireRepository,
    private val complianceAnalyzer: RepertoireComplianceAnalyzer
) : ViewModel() {

    private val _isRebuildingIndex = MutableStateFlow(false)
    val isRebuildingIndex: StateFlow<Boolean> = _isRebuildingIndex.asStateFlow()

    val courses: StateFlow<List<CourseProgress>> =
        repertoireRepository.getAllRepertoires().mapLatest { repertoires ->
            repertoires.map { rep ->
                val total = repertoireRepository.getLineCountForRepertoire(rep.id)
                val learned = repertoireRepository.getLearnedLineCountForRepertoire(rep.id)
                CourseProgress(
                    repertoire = rep, totalLines = total, learnedLines = learned
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addRepertoire(name: String, color: String) {
        viewModelScope.launch {
            repertoireRepository.insertRepertoire(Repertoire(name = name, color = color))
        }
    }

    fun deleteRepertoire(repertoire: Repertoire) {
        viewModelScope.launch {
            repertoireRepository.deleteRepertoire(repertoire)
        }
    }

    fun rebuildComplianceIndex() {
        viewModelScope.launch {
            _isRebuildingIndex.value = true
            try {
                complianceAnalyzer.rebuildIndex(playerIsWhite = true)
                complianceAnalyzer.rebuildIndex(playerIsWhite = false)
            } finally {
                _isRebuildingIndex.value = false
            }
        }
    }

    class Factory(
        private val repertoireRepository: RepertoireRepository,
        private val complianceAnalyzer: RepertoireComplianceAnalyzer
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return RepertoiresViewModel(repertoireRepository, complianceAnalyzer) as T
        }
    }
}
