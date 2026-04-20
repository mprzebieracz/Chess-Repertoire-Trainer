package com.example.chessrepertoiretrainer.feature.openingtree.data

/**
 * Simple in-memory cache for opening trees built from a player's games.
 *
 * The cache is keyed by profile id together with the filters that were used
 * to build the tree (color, time control selection, and an optional
 * max-games limit). This allows the first screen to perform all heavy work
 * (downloading + parsing PGNs + building the tree) and subsequent screens to
 * simply display the prepared tree.
 */
data class OpeningTreeCacheKey(
    val profileId: Long, val color: String, val timeControlFilter: String, val maxGamesForTree: Int?
)

object OpeningTreeCache {

    private val cache = mutableMapOf<OpeningTreeCacheKey, OpeningTree>()

    @Synchronized
    fun put(key: OpeningTreeCacheKey, tree: OpeningTree) {
        cache[key] = tree
    }

    @Synchronized
    fun get(key: OpeningTreeCacheKey): OpeningTree? = cache[key]

    /**
     * Remove all cached trees associated with the given profile. This is
     * useful after re-syncing games so that stale trees are not reused.
     */
    @Synchronized
    fun clearForProfile(profileId: Long) {
        val iterator = cache.keys.iterator()
        while (iterator.hasNext()) {
            if (iterator.next().profileId == profileId) {
                iterator.remove()
            }
        }
    }

}

