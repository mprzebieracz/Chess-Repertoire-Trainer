package com.example.chessrepertoiretrainer.ui.viewmodels

import com.example.chessrepertoiretrainer.data.FetchedGame
import com.example.chessrepertoiretrainer.data.GameWithPgn
import com.example.chessrepertoiretrainer.ui.viewmodels.OpeningTreeFilterUtils.filterGames

/**
 * Shared helpers for applying opening-tree filters (color + time control)
 * to lists of [GameWithPgn]. The semantics are the same across
 * [OpeningTreeViewModel], [MyStatsViewModel] and other viewmodels that
 * prepare games for opening-tree analysis.
 */
object OpeningTreeFilterUtils {

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
        var sequence = games.asSequence()

        // Color filter.
        sequence = when (colorFilter) {
            OpeningTreeViewModel.ColorFilter.BOTH -> sequence
            OpeningTreeViewModel.ColorFilter.WHITE_ONLY -> sequence.filter { it.game.isUserWhite }
            OpeningTreeViewModel.ColorFilter.BLACK_ONLY -> sequence.filter { !it.game.isUserWhite }
        }

        // Time-control filter.
        val tcRaw = timeControlFilter?.trim().orEmpty()
        if (tcRaw.isNotEmpty()) {
            val categories = tcRaw.split(',')
                .map { it.trim().lowercase() }
                .filter { it.isNotEmpty() }
                .toSet()

            if (categories.isNotEmpty()) {
                sequence = sequence.filter { gwp ->
                    matchesTimeControlFilter(gwp.game.timeCategory, categories)
                }
            }
        }

        return sequence.toList()
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
        games: List<FetchedGame>,
        color: String,
        timeControlFilter: String
    ): List<FetchedGame> {
        var sequence = games.asSequence()

        // Color filter.
        sequence = when (color) {
            "white" -> sequence.filter { it.isUserWhite }
            "black" -> sequence.filter { !it.isUserWhite }
            else -> sequence
        }

        // Time-control filter.
        val tcRaw = timeControlFilter
        if (tcRaw.isNotEmpty()) {
            val categories = tcRaw.split(',')
                .map { it.trim().lowercase() }
                .filter { it.isNotEmpty() }
                .toSet()

            if (categories.isNotEmpty()) {
                sequence = sequence.filter { fetched ->
                    matchesTimeControlFilter(fetched.timeCategory, categories)
                }
            }
        }

        return sequence.toList()
    }
}

