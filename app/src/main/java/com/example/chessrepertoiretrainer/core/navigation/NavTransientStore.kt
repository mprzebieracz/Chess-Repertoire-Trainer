package com.example.chessrepertoiretrainer.core.navigation

import com.example.chessrepertoiretrainer.core.database.entity.SavedGame
import com.example.chessrepertoiretrainer.feature.openingtree.data.OpeningTree

/**
 * Temporary holder for non-serializable objects passed between navigation destinations.
 * Each `take*` call reads the value and clears it, so only the first consumer gets it.
 * All fields are @Volatile for cross-thread visibility (nav callbacks may run on background threads).
 */
class NavTransientStore {

    @Volatile
    var openingTree: OpeningTree? = null

    @Volatile
    var savedGame: SavedGame? = null

    @Volatile
    var selectedChapterIds: List<Int>? = null

    @Volatile
    var analysisStartFen: String? = null

    fun takeOpeningTree(): OpeningTree? = openingTree.also { openingTree = null }
    fun takeSavedGame(): SavedGame? = savedGame.also { savedGame = null }
    fun takeSelectedChapterIds(): List<Int>? = selectedChapterIds.also { selectedChapterIds = null }
    fun takeAnalysisStartFen(): String? = analysisStartFen.also { analysisStartFen = null }
}