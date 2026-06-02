package com.example.chessrepertoiretrainer.feature.puzzles.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.chessrepertoiretrainer.core.database.entity.RepertoireOpening
import com.example.chessrepertoiretrainer.core.opening.OpeningClassifier

import com.example.chessrepertoiretrainer.core.opening.OpeningRegistry
import com.example.chessrepertoiretrainer.feature.puzzles.PuzzleRepository
import com.example.chessrepertoiretrainer.feature.repertoire.domain.RepertoireRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RepertoirePuzzlesViewModel(
    private val puzzleRepository: PuzzleRepository,
    private val repertoireRepository: RepertoireRepository,
    private val openingRegistry: OpeningRegistry,
) : ViewModel() {

    data class OpeningUiItem(
        val family: String,
        val eco: String?,
        val color: String,
        val unsolvedCount: Int,
        val isSelected: Boolean = false,
    )

    data class UiState(
        val isLoading: Boolean = true,
        val isScanning: Boolean = false,
        val openings: List<OpeningUiItem> = emptyList(),
        val statusMessage: String? = null,
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // Fast path: show stored openings immediately
            val stored = puzzleRepository.getRepertoireOpenings()
            val items = buildItems(stored)
            _uiState.update { it.copy(isLoading = false, openings = items) }

            // Background: re-scan repertoire and sync the stored list
            scanAndSync()
        }
    }

    private suspend fun scanAndSync() {
        _uiState.update { it.copy(isScanning = true) }
        try {
            val detected = scanRepertoireOpenings()
            puzzleRepository.syncRepertoireOpenings(detected)

            // Reload list (may have new or removed openings)
            val updated = puzzleRepository.getRepertoireOpenings()
            val items = buildItems(updated)
            _uiState.update { it.copy(openings = preserveSelections(items)) }

            // Fetch puzzles for any opening with an empty pool
            val needsFetch = updated.filter { o ->
                puzzleRepository.countUnsolvedForFamily(o.family) == 0
            }
            if (needsFetch.isNotEmpty()) {
                puzzleRepository.fetchAndSaveOpeningPuzzles(needsFetch)
                // Reload counts after fetch
                val refreshed = puzzleRepository.getRepertoireOpenings()
                _uiState.update { it.copy(openings = preserveSelections(buildItems(refreshed))) }
            }
        } finally {
            _uiState.update { it.copy(isScanning = false) }
        }
    }

    private suspend fun buildItems(openings: List<RepertoireOpening>): List<OpeningUiItem> =
        openings.map { o ->
            OpeningUiItem(
                family = o.family,
                eco = o.eco,
                color = o.color,
                unsolvedCount = puzzleRepository.countUnsolvedForFamily(o.family),
            )
        }

    private fun preserveSelections(newItems: List<OpeningUiItem>): List<OpeningUiItem> {
        val selectedFamilies = _uiState.value.openings.filter { it.isSelected }.map { it.family }.toSet()
        return newItems.map { it.copy(isSelected = it.family in selectedFamilies) }
    }

    fun toggleSelection(family: String) {
        _uiState.update { state ->
            state.copy(openings = state.openings.map { item ->
                if (item.family == family) item.copy(isSelected = !item.isSelected) else item
            })
        }
    }

    fun selectedFamilies(): List<String> =
        _uiState.value.openings.filter { it.isSelected }.map { it.family }

    fun selectedOpenings(): List<RepertoireOpening> =
        _uiState.value.openings.filter { it.isSelected }
            .map { RepertoireOpening(family = it.family, eco = it.eco, color = it.color) }

    fun totalUnsolvedInSelection(): Int =
        _uiState.value.openings.filter { it.isSelected }.sumOf { it.unsolvedCount }

    fun manualRefresh() {
        viewModelScope.launch { scanAndSync() }
    }

    private suspend fun scanRepertoireOpenings(): List<RepertoireOpening> {
        val fensByLine = repertoireRepository.getAllLineMoveFens()
        val colorByLine = repertoireRepository.getAllLineColors()
        val detected = mutableMapOf<String, RepertoireOpening>()
        for ((lineId, fens) in fensByLine) {
            val entry = OpeningClassifier.classifyByFenHistory(fens, openingRegistry) ?: continue
            repertoireRepository.updateLineEcoCode(lineId, entry.eco)
            val color = colorByLine[lineId]?.lowercase() ?: "white"
            detected[entry.family] = RepertoireOpening(
                family = entry.family,
                eco = entry.eco,
                color = color,
            )
        }
        return detected.values.toList()
    }

    class Factory(
        private val puzzleRepository: PuzzleRepository,
        private val repertoireRepository: RepertoireRepository,
        private val openingRegistry: OpeningRegistry,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            RepertoirePuzzlesViewModel(puzzleRepository, repertoireRepository, openingRegistry) as T
    }
}
