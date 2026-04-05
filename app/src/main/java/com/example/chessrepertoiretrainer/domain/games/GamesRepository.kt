package com.example.chessrepertoiretrainer.domain.games

import com.example.chessrepertoiretrainer.data.GameSyncResult
import com.example.chessrepertoiretrainer.data.GameWithPgn
import com.example.chessrepertoiretrainer.database.entities.Game
import kotlinx.coroutines.flow.Flow

/**
 * Domain-level abstraction for accessing and synchronizing player games.
 *
 * This interface sits between viewmodels and the concrete data layer so that
 * future changes to storage, sync strategy, or analysis do not leak into the
 * UI layer.
 */
interface GamesRepository {

    /** Reactive stream of games for a given profile, ordered by playedAt desc. */
    fun getGamesForProfile(profileId: Long): Flow<List<Game>>

    /** Snapshot helper returning games together with their stored PGN. */
    suspend fun getGamesWithPgnForProfile(
        profileId: Long,
        maxGames: Int? = null
    ): List<GameWithPgn>

    /**
     * Download/synchronize games for the given profile and persist them
     * locally. The implementation is free to apply incremental sync based on
     * last sync time and remote platform semantics.
     */
    suspend fun syncGamesForProfile(
        profileId: Long,
        maxGames: Int? = null
    ): GameSyncResult
}

