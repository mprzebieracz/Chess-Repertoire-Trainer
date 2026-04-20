package com.example.chessrepertoiretrainer.feature.openingtree.presentation

import com.example.chessrepertoiretrainer.feature.openingtree.data.FetchedGame
import com.example.chessrepertoiretrainer.feature.openingtree.data.GameWithPgn

/**
 * Shared helpers for applying opening-tree filters (color + time control)
 * to lists of [GameWithPgn]. The semantics are the same across
 * [OpeningTreeViewModel], [com.example.chessrepertoiretrainer.feature.stats.presentation.MyStatsViewModel] and other viewmodels that
 * prepare games for opening-tree analysis.
 */
object OpeningTreeFilterUtils {

    /**
     * Small test-friendly helper that checks if game category matches selected
     * time-control categories.
     */
    fun matchesTimeControlFilter(
        gameTimeCategory: String?, selectedCategories: Set<String>
    ): Boolean = isTimeControlMatch(gameTimeCategory, selectedCategories)

    /**
     * Apply color and time-control filters to the given [games] using the
     * provided [colorFilter] and [timeControlFilter].
     *
     * @param games Full list of games for a profile.
     * @param colorFilter Color filter as used by [OpeningTreeViewModel].
     * @param timeControlFilter Comma-separated list of time categories
     * (e.g. "bullet,blitz"), or null/blank for no time-control filtering.
     */
    fun filterGames(
        games: List<GameWithPgn>,
        colorFilter: OpeningTreeViewModel.ColorFilter,
        timeControlFilter: String?
    ): List<GameWithPgn> {
        return applyTimeControlFilterForGames(
            applyColorFilterForGames(games.asSequence(), colorFilter),
            timeControlFilter
        ).toList()
    }

    /**
     * Variant of [filterGames] that uses a normalized color string.
     */
    fun filterGamesByColor(
        games: List<GameWithPgn>, color: String, timeControlFilter: String
    ): List<GameWithPgn> {
        return applyTimeControlFilterForGames(
            applyColorFilterForGames(games.asSequence(), color),
            timeControlFilter
        ).toList()
    }

    /**
     * Apply color and time-control filters to a list of [FetchedGame] using
     * the same semantics as [filterGames], but operating on games fetched
     * directly from the network (not yet persisted).
     *
     * @param games Full list of fetched games for a user.
     * @param color Normalized color filter string: "white", "black", or
     * "both".
     * @param timeControlFilter Normalized time-control filter string
     * (already trimmed, may be empty for no explicit filter).
     */
    fun filterFetchedGames(
        games: List<FetchedGame>, color: String, timeControlFilter: String
    ): List<FetchedGame> {
        return applyTimeControlFilterForFetchedGames(
            applyColorFilterForFetchedGames(games.asSequence(), color),
            timeControlFilter
        ).toList()
    }

    private fun applyColorFilterForGames(
        sequence: Sequence<GameWithPgn>,
        colorFilter: OpeningTreeViewModel.ColorFilter
    ): Sequence<GameWithPgn> {
        return when (colorFilter) {
            OpeningTreeViewModel.ColorFilter.BOTH -> sequence
            OpeningTreeViewModel.ColorFilter.WHITE_ONLY -> sequence.filter { it.game.isUserWhite }
            OpeningTreeViewModel.ColorFilter.BLACK_ONLY -> sequence.filter { !it.game.isUserWhite }
        }
    }

    private fun applyColorFilterForGames(
        sequence: Sequence<GameWithPgn>,
        color: String
    ): Sequence<GameWithPgn> {
        return when (color) {
            "white" -> sequence.filter { it.game.isUserWhite }
            "black" -> sequence.filter { !it.game.isUserWhite }
            else -> sequence
        }
    }

    private fun applyColorFilterForFetchedGames(
        sequence: Sequence<FetchedGame>,
        color: String
    ): Sequence<FetchedGame> {
        return when (color) {
            "white" -> sequence.filter { it.isUserWhite }
            "black" -> sequence.filter { !it.isUserWhite }
            else -> sequence
        }
    }

    private fun applyTimeControlFilterForGames(
        sequence: Sequence<GameWithPgn>, timeControlFilter: String?
    ): Sequence<GameWithPgn> {
        val categories = parseTimeControlCategories(timeControlFilter?.trim().orEmpty())
        if (categories.isEmpty()) return sequence

        return sequence.filter { gwp ->
            isTimeControlMatch(gwp.game.timeCategory, categories)
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