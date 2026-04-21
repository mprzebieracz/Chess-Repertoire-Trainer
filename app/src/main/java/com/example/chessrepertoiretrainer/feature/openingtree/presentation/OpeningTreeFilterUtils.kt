package com.example.chessrepertoiretrainer.feature.openingtree.presentation

import com.example.chessrepertoiretrainer.feature.openingtree.data.fetcher.FetchedGame

object OpeningTreeFilterUtils {

    fun matchesTimeControlFilter(
        gameTimeCategory: String?, selectedCategories: Set<String>
    ): Boolean = isTimeControlMatch(gameTimeCategory, selectedCategories)

    fun filterFetchedGames(
        games: List<FetchedGame>, color: String, timeControlFilter: String
    ): List<FetchedGame> {
        return applyTimeControlFilterForFetchedGames(
            applyColorFilterForFetchedGames(games.asSequence(), color), timeControlFilter
        ).toList()
    }

    private fun applyColorFilterForFetchedGames(
        sequence: Sequence<FetchedGame>, color: String
    ): Sequence<FetchedGame> {
        return when (color) {
            "white" -> sequence.filter { it.isUserWhite }
            "black" -> sequence.filter { !it.isUserWhite }
            else -> sequence
        }
    }

    private fun applyTimeControlFilterForFetchedGames(
        sequence: Sequence<FetchedGame>, timeControlFilter: String
    ): Sequence<FetchedGame> {
        val categories = parseTimeControlCategories(timeControlFilter)
        if (categories.isEmpty()) return sequence

        return sequence.filter { fetched ->
            isTimeControlMatch(fetched.timeCategory, categories)
        }
    }

    private fun parseTimeControlCategories(raw: String): Set<String> {
        if (raw.isBlank()) return emptySet()
        return raw.split(',').map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()
    }

    private fun isTimeControlMatch(
        gameTimeCategory: String?, selectedCategories: Set<String>
    ): Boolean {
        if (selectedCategories.isEmpty()) return true
        val category = gameTimeCategory?.trim()?.lowercase() ?: return false
        return category in selectedCategories
    }
}