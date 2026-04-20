package com.example.chessrepertoiretrainer.data

import android.util.Log
import com.example.chessrepertoiretrainer.database.dao.GameDao
import com.example.chessrepertoiretrainer.database.dao.PlayerProfileDao
import com.example.chessrepertoiretrainer.database.entities.Game
import com.example.chessrepertoiretrainer.database.entities.GameMoves
import com.example.chessrepertoiretrainer.domain.games.GamesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Repository responsible for downloading games for a tracked [PlayerProfile]
 * from external providers and storing them locally using [GameDao].
 */
class PlayerGamesRepository(
    private val gameDao: GameDao, private val playerProfileDao: PlayerProfileDao, private val fetcherRegistry: GameFetcherRegistry
) : GamesRepository {

    override fun getGamesForProfile(profileId: Long): Flow<List<Game>> = gameDao.getGamesForProfile(profileId)

    /**
     * Snapshot helper returning games for a profile together with their
     * stored PGN, for use by opening-tree style analyses.
     */
    override suspend fun getGamesWithPgnForProfile(
        profileId: Long, maxGames: Int?
    ): List<GameWithPgn> {
        val games: List<Game> = if (maxGames != null && maxGames > 0) {
            gameDao.getLastNGamesForProfile(profileId, maxGames)
        }
        else {
            gameDao.getGamesForProfile(profileId).first()
        }
        if (games.isEmpty()) return emptyList()

        val result = mutableListOf<GameWithPgn>()
        for (game in games) {
            val moves = gameDao.getGameMoves(game.id) ?: continue
            result += GameWithPgn(game = game, pgn = moves.pgn)
        }
        return result
    }

    /**
     * Download games for the given profile and persist them in the local
     * database. The operation is incremental based on
     * [PlayerProfile.lastSyncTime] when available.
     */
    override suspend fun syncGamesForProfile(
        profileId: Long, maxGames: Int?
    ): GameSyncResult {
        val profile = playerProfileDao.getProfileById(profileId) ?: return GameSyncResult(0, 0, errorMessage = "Profile not found")

        val fetcher = fetcherRegistry.getFetcher(profile.platform) ?: return GameSyncResult(
            0, 0, errorMessage = "Platform '${profile.platform}' not supported yet"
        )

        val since = profile.lastSyncTime
        Log.d(
            "PlayerGamesRepository",
            "Syncing games for profile id=${profile.id}, username=${profile.username}, " + "platform=${profile.platform}, since=$since, maxGames=$maxGames"
        )

        val fetched = try {
            fetcher.fetchGamesForUser(profile.username, since = since, maxGames = maxGames)
        }
        catch (e: Exception) {
            Log.e("PlayerGamesRepository", "Error fetching games: ${e.message}", e)
            return GameSyncResult(0, 0, errorMessage = "Failed to download games: ${e.message}")
        }

        if (fetched.isEmpty()) {
            return GameSyncResult(newGames = 0, totalFetched = 0, errorMessage = null)
        }

        var newGamesCount = 0
        var latestPlayedAt = since ?: 0L

        for (g in fetched.sortedBy { it.playedAt }) {
            if (g.playedAt > latestPlayedAt) {
                latestPlayedAt = g.playedAt
            }

            val existingId = gameDao.findGameId(
                profileId = profile.id, platformGameId = g.platformGameId
            )
            if (existingId != null) {
                // Game already stored for this profile.
                continue
            }

            val entity = Game(
                platformGameId = g.platformGameId,
                profileId = profile.id,
                opponentName = g.opponentName,
                isUserWhite = g.isUserWhite,
                result = g.result,
                timeControl = g.timeControl,
                timeCategory = g.timeCategory,
                rated = g.rated,
                playedAt = g.playedAt
            )
            val moves = GameMoves(
                gameId = 0, pgn = g.pgn
            )

            try {
                gameDao.insertGameWithMoves(entity, moves)
                newGamesCount++
            }
            catch (e: Exception) {
                Log.e(
                    "PlayerGamesRepository", "Failed to insert game ${g.platformGameId} for profile ${profile.id}: ${e.message}", e
                )
            }
        }

        if (latestPlayedAt > (since ?: 0L)) {
            try {
                playerProfileDao.updateProfile(profile.copy(lastSyncTime = latestPlayedAt))
            }
            catch (e: Exception) {
                Log.e(
                    "PlayerGamesRepository", "Failed to update lastSyncTime for profile ${profile.id}: ${e.message}", e
                )
            }
        }

        return GameSyncResult(
            newGames = newGamesCount, totalFetched = fetched.size, errorMessage = null
        )
    }
}

data class GameWithPgn(
    val game: Game, val pgn: String
)

data class GameSyncResult(
    val newGames: Int, val totalFetched: Int, val errorMessage: String? = null
) {
    val isSuccess: Boolean
        get() = errorMessage == null
}

