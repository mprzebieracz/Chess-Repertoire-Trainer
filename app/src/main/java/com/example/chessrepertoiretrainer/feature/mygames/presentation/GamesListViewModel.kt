package com.example.chessrepertoiretrainer.feature.mygames.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.chessrepertoiretrainer.core.database.entity.SavedGame
import com.example.chessrepertoiretrainer.feature.mygames.data.SavedGameRepository
import com.example.chessrepertoiretrainer.feature.mygames.domain.model.GameFilter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class GamesListViewModel(private val repository: SavedGameRepository) : ViewModel() {

    private val _filter = MutableStateFlow(GameFilter())
    val filter: StateFlow<GameFilter> = _filter.asStateFlow()

    val games: StateFlow<List<SavedGame>> = _filter.flatMapLatest { f ->
        repository.getAllGamesFiltered(platform = f.platform,
                                       result = f.playerResult,
                                       isWhite = f.isPlayerWhite).map { games ->
            if (f.timeCategories.isEmpty()) games
            else games.filter { it.timeCategory in f.timeCategories }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setFilter(filter: GameFilter) {
        _filter.value = filter
    }

    fun toggleTimeCategory(category: String) {
        val current = _filter.value.timeCategories
        _filter.value =
            _filter.value.copy(timeCategories = if (category in current) current - category else current + category)
    }

    class Factory(private val repository: SavedGameRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            return GamesListViewModel(repository) as T
        }
    }
}